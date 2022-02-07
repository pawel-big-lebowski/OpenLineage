package io.openlineage.spark.agent.util;

import static io.openlineage.spark.agent.util.ScalaConversionUtils.asJavaOptional;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.delta.files.TahoeLogFileIndex;
import org.apache.spark.sql.execution.datasources.HadoopFsRelation;
import org.apache.spark.sql.execution.datasources.LogicalRelation;

@Slf4j
public class DatasetVersionDatasetFacetUtils {

  /**
   * Delta uses LogicalRelation's HadoopFsRelation as a logical plan's leaf. It implements FileIndex
   * using TahoeLogFileIndex that contains DeltaLog, which can be used to get dataset's snapshot.
   */
  public static Optional<String> extractVersionFromLogicalRelation(
      LogicalRelation logicalRelation) {
    if (logicalRelation.relation() instanceof HadoopFsRelation) {
      HadoopFsRelation fsRelation = (HadoopFsRelation) logicalRelation.relation();
      asJavaOptional(logicalRelation.catalogTable());
      if (logicalRelation.catalogTable().isDefined()
          && logicalRelation.catalogTable().get().provider().isDefined()
          && logicalRelation.catalogTable().get().provider().get().equalsIgnoreCase("delta")) {
        if (hasDeltaClasses() && fsRelation.location() instanceof TahoeLogFileIndex) {
          TahoeLogFileIndex fileIndex = (TahoeLogFileIndex) fsRelation.location();
          return Optional.of(Long.toString(fileIndex.getSnapshot().version()));
        }
      }
    }
    return Optional.empty();
  }

  protected static boolean hasDeltaClasses() {
    try {
      DatasetVersionDatasetFacetUtils.class
          .getClassLoader()
          .loadClass("org.apache.spark.sql.delta.files.TahoeLogFileIndex");
      return true;
    } catch (Exception e) {
      // swallow- we don't care
    }
    return false;
  }
}
