package io.openlineage.spark3.agent.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.openlineage.spark3.agent.lifecycle.plan.catalog.CatalogUtils3;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import scala.Option;

public class DatasetVersionDatasetFacetUtilsTest {

  DataSourceV2Relation v2Relation = mock(DataSourceV2Relation.class);
  Identifier identifier = mock(Identifier.class);
  TableCatalog tableCatalog = mock(TableCatalog.class);
  Table table = mock(Table.class);
  Map<String, String> tableProperties = new HashMap<>();

  @Test
  public void testExtractVersionFromDataSourceV2RelationWhenNoIdentifier() {
    when(v2Relation.identifier()).thenReturn(Option.empty());
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(v2Relation));
  }

  @Test
  public void testExtractVersionFromDataSourceV2RelationWhenNoCatalog() {
    when(v2Relation.identifier()).thenReturn(Option.apply(identifier));
    when(v2Relation.catalog()).thenReturn(Option.empty());
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(v2Relation));
  }

  @Test
  public void testExtractVersionFromDataSourceV2RelationWhenCatalogIsNotTableCatalog() {
    when(v2Relation.identifier()).thenReturn(Option.apply(identifier));
    when(v2Relation.catalog()).thenReturn(Option.apply(mock(CatalogPlugin.class)));
    assertEquals(
        Optional.empty(),
        DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(v2Relation));
  }

  @Test
  public void testExtractVersionFromDataSourceV2Relation() {
    when(v2Relation.identifier()).thenReturn(Option.apply(identifier));
    when(v2Relation.catalog()).thenReturn(Option.apply(tableCatalog));
    when(v2Relation.table()).thenReturn(table);
    when(table.properties()).thenReturn(tableProperties);

    try (MockedStatic<CatalogUtils3> mocked = mockStatic(CatalogUtils3.class)) {
      when(CatalogUtils3.getDatasetVersion(tableCatalog, identifier, tableProperties))
          .thenReturn(Optional.of("some-version"));
      assertEquals(
          Optional.of("some-version"),
          DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(v2Relation));
    }
  }
}
