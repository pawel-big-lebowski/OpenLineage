package io.openlineage.spark3.agent.utils;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.agent.util.ScalaConversionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.sql.catalyst.expressions.Attribute;
import org.apache.spark.sql.catalyst.expressions.AttributeReference;
import org.apache.spark.sql.catalyst.expressions.ExprId;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.catalyst.plans.logical.Union;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanRelation;

/**
 * Utility functions for detecting column level lineage within {@link
 * org.apache.spark.sql.catalyst.plans.logical.LogicalPlan}.
 */
@Slf4j
public class ColumnLevelLineageUtils {

  // FIXME: implement test that checks the behaviour on different SQL queries

  public static void includeColumnLevelLineage(
      LogicalPlan plan, OpenLineage.DatasetFacetsBuilder datasetFacetsBuilder) {

    // inputs
    Map<ExprId, ExprId> exprMap = new HashMap<>();
    Map<ExprId, List<Pair<DataSourceV2Relation, String>>> inputs = discoverInputs(plan, exprMap);

    log.info("inputs");
    inputs
        .keySet()
        .forEach(
            key -> {
              inputs.get(key).stream()
                  .forEach(
                      pair ->
                          log.info(
                              "input: table {}, field{}, exprId",
                              pair.getLeft().name(),
                              pair.getRight(),
                              key));
            });

    // exprMap
    exprMap.keySet().forEach(key -> log.info("exprMap {} -> {}", key.id(), exprMap.get(key).id()));

    // outputs
    Map<ExprId, String> outputs = new HashMap<>();
    discoverOutputs(plan, outputs);

    log.info("outputs");
    outputs
        .keySet()
        .forEach(
            key -> {
              log.info("output: {} -> {}", key, String.join(".", outputs.get(key)));
            });

    log.info(plan.prettyJson());
  }

  private static Map<ExprId, String> discoverOutputs(
      LogicalPlan plan, Map<ExprId, String> outputs) {
    if (!outputs.isEmpty()) {
      return outputs;
    }

    ScalaConversionUtils.fromSeq(plan.output()).stream()
        .filter(attr -> attr instanceof Attribute)
        .map(attr -> (Attribute) attr)
        .forEach(attr -> outputs.put(attr.exprId(), attr.name()));

    if (outputs.isEmpty()) {
      // try to extract it deeper
      plan.children()
          .foreach(
              child -> {
                outputs.putAll(discoverOutputs(child, outputs));
                return scala.runtime.BoxedUnit.UNIT;
              });
    }

    return outputs;
  }

  private static Map<ExprId, List<Pair<DataSourceV2Relation, String>>> discoverInputs(
      LogicalPlan plan, Map<ExprId, ExprId> exprMap) {
    Map<ExprId, List<Pair<DataSourceV2Relation, String>>> inputs = new HashMap<>();

    plan.children()
        .foreach(
            node -> {
              node.foreach(
                  innerNode -> {
                    Optional.of(innerNode)
                        .ifPresent(
                            e -> {
                              if (e instanceof Union) {
                                handleUnion((Union) e, exprMap);
                              }
                              ScalaConversionUtils.fromSeq(e.output()).stream()
                                  .filter(attr -> attr instanceof AttributeReference)
                                  .map(attr -> (AttributeReference) attr)
                                  .forEach(
                                      attr -> {
                                        if (!inputs.containsKey(attr.exprId())) {
                                          inputs.put(attr.exprId(), new LinkedList<>());
                                        }
                                        extractAttribute(e, attr)
                                            .ifPresent(p -> inputs.get(attr.exprId()).add(p));
                                      });
                            });
                    return scala.runtime.BoxedUnit.UNIT;
                  });
              return scala.runtime.BoxedUnit.UNIT;
            });

    return inputs;
  }

  private static Optional<Pair<DataSourceV2Relation, String>> extractAttribute(
      LogicalPlan node, AttributeReference attr) {
    if (node instanceof DataSourceV2Relation) {
      return Optional.of(Pair.of((DataSourceV2Relation) node, attr.name()));
    } else if (node instanceof DataSourceV2ScanRelation) {
      return Optional.of(Pair.of(((DataSourceV2ScanRelation) node).relation(), attr.name()));
    }
    return Optional.empty();
  }

  private static void handleUnion(Union union, Map<ExprId, ExprId> exprMap) {
    // implement in Java code equivalent to Scala 'children.map(_.output).transpose.map { attrs =>'
    List<LogicalPlan> children = ScalaConversionUtils.<LogicalPlan>fromSeq(union.children());
    List<ArrayList<Attribute>> childrenAttributes = new LinkedList<>();
    children.forEach(
        c ->
            childrenAttributes.add(
                new ArrayList<>(ScalaConversionUtils.<Attribute>fromSeq(c.output()))));

    // max attributes size
    int maxAttributeSize =
        childrenAttributes.stream().map(l -> l.size()).max(Integer::compare).get();

    IntStream.range(0, maxAttributeSize)
        .forEach(
            position -> {
              ExprId firstExpr = childrenAttributes.get(0).get(position).exprId();
              IntStream.range(1, children.size())
                  .forEach(
                      childIndex -> {
                        ArrayList<Attribute> attributes = childrenAttributes.get(childIndex);
                        if (attributes.size() >= position) {
                          exprMap.put(attributes.get(position).exprId(), firstExpr);
                        }
                      });
            });
  }
}
