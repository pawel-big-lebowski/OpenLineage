---
Status: IN PROGRESS  
Author: Julien Le Dem (original issue) / Maciej Obuchowski (this document)  
Created: 2021-10-18  
Issue: [269](https://github.com/OpenLineage/OpenLineage/issues/269)
---

##Purpose

The goal is to facilitate sending `OpenLineage` events over various protocols. 
(HTTP, queues, ...) independently of the logic via exposing `Transport` interface.

## Proposed interface

In pseudocode:
```
interface Transport {
  send(LineageEvent event);
}
```

The send method should be synchronous and return when underlying system confirms 
that message has been received. This can be for example HTTP response in 200 range, 
successful fsync on file descriptor, or successful Apache Kafka flush.

Potential error handling is deliberately unspecified on overall interface level, 
but should be specified and consistent for particular language implementation. 
The reason for that is difference between idiomatic error handling between different 
languages: while Python and Java prefer exceptions, Go prefers multiple return values, 
and Rust prefers monadic `Result<T,E>`. 

However, `Clients` and `Transports` utilizing exceptions should be careful to not directly expose 
integrations to exceptions thrown by underlying libraries - integrations should be 
independent of `Transports`. 


`Transports` are implemented for particular language's `Client`. There is no guarantee 
of availability of any `Transport` on instance that implementation exist for different 
language, with following exception.
Any reference implementation of `OpenLineage` `Client` should include default 
`HTTP Transport`. 

## Instantiation

Integration code instantiation is usually controlled by system for which they are 
written - for example Airflow spawns `LineageBackend` instance. Then, integration
code would spawn `Client`, which would be responsible for spawning relevant 
`Transport`.  
This is contrary to usual dependency injection approach, and creates two issues:   
that `Client` needs to be aware of which `Transport` to spawn;
and that `Transport` instantiation should be lightweight and not depend on variables
passed to it's constructor.  
Former issue can be solved by standarizing `OPENLINEAGE_TRANSPORT` environment 
variable notifying `Client` of which `Transport` to spawn.  
Second issue could be solved by environment variables defined by particular `Transport` 
itself. For systems that have large number of potential configuration options, like 
`Apache Kafka`, that environment variable could point to some config file where 
configuration resides. Same `Transport` implementations for different languages should
be careful to keep configuration consistent, however, some differences are accepted
if it improves user experience. 


To keep backwards compatibility, in absence of `OPENLINEAGE_TRANSPORT` header `Clients`
should assume that `HTTP` transport is used and it utilizes `OPENLINEAGE_URL` 
or `OPENLINEAGE_API_KEY` environment variables.

## Relation to ProxyBackend

`ProxyBackend` can utilize Transports to route `LineageEvents` to one or more systems.
It's written in Java, so it can utilize only `Transports` that are written in 
that language.  
`ProxyBackend` could not depend on proposed environment variables, but have it's own 
configuration system.  
More details on `ProxyBackend` should be on [it's own github issue](https://github.com/OpenLineage/OpenLineage/issues/269).
