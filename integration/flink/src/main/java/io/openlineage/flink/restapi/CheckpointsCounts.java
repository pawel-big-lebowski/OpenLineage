package io.openlineage.flink.restapi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckpointsCounts {
  int completed;
  int failed;
  int in_progress;
  int restored;
  int total;
}
