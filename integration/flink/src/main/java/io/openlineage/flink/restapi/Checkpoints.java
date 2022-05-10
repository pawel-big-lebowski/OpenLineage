package io.openlineage.flink.restapi;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Checkpoints {
  CheckpointsCounts counts;
}
