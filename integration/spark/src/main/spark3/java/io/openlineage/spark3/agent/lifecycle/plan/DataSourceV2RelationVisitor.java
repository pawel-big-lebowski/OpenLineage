package io.openlineage.spark3.agent.lifecycle.plan;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.agent.util.PlanUtils;
import io.openlineage.spark.api.DatasetFactory;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark.api.QueryPlanVisitor;
import io.openlineage.spark3.agent.utils.DatasetVersionDatasetFacetUtils;
import io.openlineage.spark3.agent.utils.PlanUtils3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanRelation;

/**
 * Find {@link org.apache.spark.sql.sources.BaseRelation}s and {@link
 * org.apache.spark.sql.connector.catalog.Table}.
 *
 * <p>Note that while the {@link DataSourceV2Relation} is a {@link
 * org.apache.spark.sql.catalyst.analysis.NamedRelation}, the returned name is that of the source,
 * not the specific dataset (e.g., "bigquery" not the table).
 */
@Slf4j
public class DataSourceV2RelationVisitor<D extends OpenLineage.Dataset>
    extends QueryPlanVisitor<LogicalPlan, D> {

  private final DatasetFactory<D> factory;
  private final boolean isInputVisitor;

  public DataSourceV2RelationVisitor(
      OpenLineageContext context, DatasetFactory<D> factory, boolean isInputVisitor) {
    super(context);
    this.factory = factory;
    this.isInputVisitor = isInputVisitor;
  }

  @Override
  public boolean isDefinedAt(LogicalPlan logicalPlan) {
    return logicalPlan instanceof DataSourceV2Relation
        || logicalPlan instanceof DataSourceV2ScanRelation;
  }

  @Override
  public List<D> apply(LogicalPlan logicalPlan) {
    DataSourceV2Relation relation = getRelation(logicalPlan);
    Map<String, OpenLineage.DatasetFacet> facets = new HashMap<>();

    if (PlanUtils.shouldIncludeDatasetVersionFacet(isInputVisitor, triggeringEvent)) {
      includeDatasetVersionFacets(relation, facets);
    }

    return PlanUtils3.fromDataSourceV2Relation(factory, context, relation, facets);
  }

  private void includeDatasetVersionFacets(
      DataSourceV2Relation relation, Map<String, OpenLineage.DatasetFacet> facets) {
    DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(relation)
        .ifPresent(
            version ->
                facets.put(
                    "datasetVersion",
                    context.getOpenLineage().newDatasetVersionDatasetFacet(version)));
  }

  private DataSourceV2Relation getRelation(LogicalPlan logicalPlan) {
    if (logicalPlan instanceof DataSourceV2ScanRelation) {
      return ((DataSourceV2ScanRelation) logicalPlan).relation();
    } else {
      return (DataSourceV2Relation) logicalPlan;
    }
  }
}
