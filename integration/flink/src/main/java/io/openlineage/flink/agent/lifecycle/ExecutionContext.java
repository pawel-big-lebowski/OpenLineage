package io.openlineage.flink.agent.lifecycle;

import org.apache.flink.api.common.JobExecutionResult;

public interface ExecutionContext {

  void onJobSubmitted();

  void onJobCheckpoint();

  void onJobExecuted(JobExecutionResult jobExecutionResult);
}
