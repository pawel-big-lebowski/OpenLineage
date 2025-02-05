/*
/* Copyright 2018-2025 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark.agent.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.openlineage.spark.agent.util.SparkSessionUtils;
import io.openlineage.spark.api.OpenLineageContext;
import java.util.Optional;
import org.apache.spark.scheduler.SparkListenerApplicationStart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventFilterUtilsTest {

  EventFilterBuilder customFilterBuilder =
      new EventFilterBuilder() {
        @Override
        public EventFilter build(OpenLineageContext context) {
          throw new RuntimeException("Build called");
        }
      };

  @Test
  void testFilterIsRegistered() {
    try (MockedStatic mocked = mockStatic(SparkSessionUtils.class)) {
      when(SparkSessionUtils.activeSession()).thenReturn(Optional.empty());
      assertThat(
              EventFilterUtils.isDisabled(
                  mock(OpenLineageContext.class), mock(SparkListenerApplicationStart.class)))
          .isFalse();

      EventFilterUtils.registerEventFilterBuilder(customFilterBuilder);

      // exception should be thrown
      RuntimeException exception =
          Assertions.assertThrows(
              RuntimeException.class,
              () -> {
                EventFilterUtils.isDisabled(
                    mock(OpenLineageContext.class), mock(SparkListenerApplicationStart.class));
              });
      assertThat(exception.getMessage()).isEqualTo("Build called");
    }
  }
}
