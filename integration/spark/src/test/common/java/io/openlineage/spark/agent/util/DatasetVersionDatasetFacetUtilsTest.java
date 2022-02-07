package io.openlineage.spark.agent.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.spark.sql.catalyst.catalog.CatalogTable;
import org.apache.spark.sql.delta.Snapshot;
import org.apache.spark.sql.delta.files.TahoeLogFileIndex;
import org.apache.spark.sql.execution.datasources.FileIndex;
import org.apache.spark.sql.execution.datasources.HadoopFsRelation;
import org.apache.spark.sql.execution.datasources.LogicalRelation;
import org.apache.spark.sql.sources.BaseRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import scala.Option;

public class DatasetVersionDatasetFacetUtilsTest {

  LogicalRelation logicalRelation = mock(LogicalRelation.class);
  CatalogTable catalogTable = mock(CatalogTable.class);
  HadoopFsRelation fsRelation = mock(HadoopFsRelation.class);
  TahoeLogFileIndex tahoeLogFileIndex = mock(TahoeLogFileIndex.class);
  Snapshot snapshot = mock(Snapshot.class);

  @BeforeEach
  public void setUp() {
    when(logicalRelation.relation()).thenReturn(fsRelation);
    when(logicalRelation.catalogTable()).thenReturn(Option.apply(catalogTable));
    when(catalogTable.provider()).thenReturn(Option.apply("delta"));
    when(fsRelation.location()).thenReturn(tahoeLogFileIndex);
    when(tahoeLogFileIndex.getSnapshot()).thenReturn(snapshot);
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenNotHadoopFsRelation() {
    when(logicalRelation.relation()).thenReturn(mock(BaseRelation.class));
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenCatalogTableNotDefined() {
    when(logicalRelation.catalogTable()).thenReturn(Option.empty());
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenProviderNotDefined() {
    when(catalogTable.provider()).thenReturn(Option.empty());
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenProviderNotDelta() {
    when(catalogTable.provider()).thenReturn(Option.apply("non-delta"));
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenNoDeltaClasses() {
    try (MockedStatic mocked =
        mockStatic(DatasetVersionDatasetFacetUtils.class, Mockito.CALLS_REAL_METHODS)) {
      when(DatasetVersionDatasetFacetUtils.hasDeltaClasses()).thenReturn(false);
      assertEquals(
          Optional.empty(),
          DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
    }
  }

  @Test
  public void testExtractVersionFromLogicalRelationWhenLocationNotTahoeLogFileIndex() {
    when(fsRelation.location()).thenReturn(mock(FileIndex.class));
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }

  @Test
  public void testExtractVersionFromLogicalRelation() {
    when(snapshot.version()).thenReturn(1L);
    assertEquals(
        Optional.of("1"),
        DatasetVersionDatasetFacetUtils.extractVersionFromLogicalRelation(logicalRelation));
  }
}
