package io.openlineage.spark3.agent.utils;

import io.openlineage.spark3.agent.lifecycle.plan.catalog.CatalogUtils3;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;

@Slf4j
public class DatasetVersionDatasetFacetUtils {

  /**
   * Check if we have all the necessary properties in DataSourceV2Relation to get dataset version.
   */
  public static Optional<String> extractVersionFromDataSourceV2Relation(
      DataSourceV2Relation table) {
    if (table.identifier().isEmpty()) {
      log.warn("Couldn't find identifier for dataset in plan " + table);
      return Optional.empty();
    }
    Identifier identifier = table.identifier().get();

    if (table.catalog().isEmpty() || !(table.catalog().get() instanceof TableCatalog)) {
      log.warn("Couldn't find catalog for dataset in plan " + table);
      return Optional.empty();
    }
    TableCatalog tableCatalog = (TableCatalog) table.catalog().get();

    Map<String, String> tableProperties = table.table().properties();
    return CatalogUtils3.getDatasetVersion(tableCatalog, identifier, tableProperties);
  }
}
