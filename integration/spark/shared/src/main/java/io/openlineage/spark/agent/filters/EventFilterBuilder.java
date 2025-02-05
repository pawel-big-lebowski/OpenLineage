/*
/* Copyright 2018-2025 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/
package io.openlineage.spark.agent.filters;

import io.openlineage.spark.api.OpenLineageContext;

public interface EventFilterBuilder {

  EventFilter build(OpenLineageContext context);
}
