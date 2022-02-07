package io.openlineage.spark3.agent.lifecycle.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.agent.util.PlanUtils;
import io.openlineage.spark.api.DatasetFactory;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark3.agent.utils.DatasetVersionDatasetFacetUtils;
import io.openlineage.spark3.agent.utils.PlanUtils3;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.spark.scheduler.SparkListenerEvent;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanRelation;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class DataSourceV2RelationVisitorTest {

  private static final String SOME_VERSION = "version_1";
  OpenLineageContext openLineageContext = mock(OpenLineageContext.class);
  DatasetFactory<OpenLineage.OutputDataset> datasetFactory = mock(DatasetFactory.class);
  OpenLineage.DatasetVersionDatasetFacet facet = mock(OpenLineage.DatasetVersionDatasetFacet.class);
  OpenLineage openLineage = mock(OpenLineage.class);
  DataSourceV2RelationVisitor visitor =
      new DataSourceV2RelationVisitor(openLineageContext, datasetFactory, false);

  @Test
  void testIsDefined() {
    assertTrue(visitor.isDefinedAt(mock(DataSourceV2Relation.class)));
    assertTrue(visitor.isDefinedAt(mock(DataSourceV2ScanRelation.class)));
    assertFalse(visitor.isDefinedAt(mock(LogicalPlan.class)));
  }

  @Test
  void testApplyV2Relation() {
    DataSourceV2Relation relation = mock(DataSourceV2Relation.class);
    try (MockedStatic planUtils3MockedStatic = mockStatic(PlanUtils3.class)) {
      visitor.apply(relation);
      planUtils3MockedStatic.verify(
          () ->
              PlanUtils3.fromDataSourceV2Relation(
                  datasetFactory, openLineageContext, relation, new HashMap<>()),
          times(1));
    }
  }

  @Test
  void testApplyV2ScanRelation() {
    DataSourceV2Relation relation = mock(DataSourceV2Relation.class);
    DataSourceV2ScanRelation scanRelation = mock(DataSourceV2ScanRelation.class);
    when(scanRelation.relation()).thenReturn(relation);

    try (MockedStatic planUtils3MockedStatic = mockStatic(PlanUtils3.class)) {
      visitor.apply(scanRelation);
      planUtils3MockedStatic.verify(
          () ->
              PlanUtils3.fromDataSourceV2Relation(
                  datasetFactory, openLineageContext, relation, new HashMap<>()),
          times(1));
    }
  }

  @Test
  void testApplyDatasetVersionFacet() {
    DataSourceV2Relation relation = mock(DataSourceV2Relation.class);
    SparkListenerEvent event = mock(SparkListenerEvent.class);

    Map<String, OpenLineage.DatasetFacet> expectedFacets = new HashMap<>();
    when(openLineageContext.getOpenLineage()).thenReturn(openLineage);
    when(openLineage.newDatasetVersionDatasetFacet(SOME_VERSION)).thenReturn(facet);
    expectedFacets.put("datasetVersion", facet);

    try (MockedStatic planUtils3MockedStatic = mockStatic(PlanUtils3.class)) {
      try (MockedStatic facetUtilsMockedStatic =
          mockStatic(DatasetVersionDatasetFacetUtils.class)) {
        try (MockedStatic planUtils = mockStatic(PlanUtils.class)) {
          when(DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(relation))
              .thenReturn(Optional.of(SOME_VERSION));
          when(PlanUtils.shouldIncludeDatasetVersionFacet(false, event)).thenReturn(true);
          visitor.setTriggeringEvent(event);
          visitor.apply(relation);
          planUtils3MockedStatic.verify(
              () ->
                  PlanUtils3.fromDataSourceV2Relation(
                      datasetFactory, openLineageContext, relation, expectedFacets),
              times(1));
        }
      }
    }
  }
}
