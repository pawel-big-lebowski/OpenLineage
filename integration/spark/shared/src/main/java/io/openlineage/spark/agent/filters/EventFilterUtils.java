/*
/* Copyright 2018-2025 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark.agent.filters;

import io.openlineage.spark.agent.util.SparkSessionUtils;
import io.openlineage.spark.api.OpenLineageContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkContext;
import org.apache.spark.scheduler.SparkListenerEvent;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.QueryExecution;

@Slf4j
public class EventFilterUtils {

  private static List<EventFilterBuilder> filterBuilders =
      new ArrayList<>(
          Arrays.asList(
              context -> new DeltaEventFilter(context),
              context -> new DatabricksEventFilter(context),
              context -> new SparkNodesFilter(context),
              context -> new CreateViewFilter(context),
              context -> new AdaptivePlanEventFilter(context)));

  /**
   * Method that verifies based on OpenLineageContext and SparkListenerEvent if OpenLineage event
   * has to be sent.
   */
  public static boolean isDisabled(OpenLineageContext context, SparkListenerEvent event) {
    return filterBuilders.stream()
        .map(builder -> builder.build(context))
        .anyMatch(
            filter -> {
              boolean isDisabled = filter.isDisabled(event.getClass().cast(event));
              if (isDisabled) {
                String logicalPlanNode =
                    getLogicalPlan(context)
                        .map(plan -> plan.getClass().getCanonicalName())
                        .orElse("UnparsableLogicalPlan");
                if (log.isDebugEnabled()) {
                  log.debug(
                      "Rejecting event : {} with plan : {} due to filter : {}",
                      event.toString(),
                      logicalPlanNode,
                      filter.getClass().getCanonicalName());
                }
              }
              return isDisabled;
            });
  }

  /** Method that allows registering custom extra filter programmatically. */
  public static void registerEventFilterBuilder(EventFilterBuilder builder) {
    filterBuilders.add(builder);
  }

  static Optional<LogicalPlan> getLogicalPlan(OpenLineageContext context) {
    return context.getQueryExecution().map(QueryExecution::optimizedPlan);
  }

  /**
   * Verifies if `spark.sql.extensions` is set in Spark configuration and checks if it is a delta
   * extension.
   */
  static boolean isDeltaPlan() {
    return SparkSessionUtils.activeSession()
        .map(SparkSession::sparkContext)
        .map(SparkContext::conf)
        .map(conf -> conf.get("spark.sql.extensions", ""))
        .filter("io.delta.sql.DeltaSparkSessionExtension"::equals)
        .isPresent();
  }
}
