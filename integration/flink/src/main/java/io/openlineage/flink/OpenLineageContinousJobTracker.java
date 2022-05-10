package io.openlineage.flink;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openlineage.flink.agent.lifecycle.FlinkExecutionContext;
import io.openlineage.flink.restapi.Checkpoints;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

@Slf4j
public class OpenLineageContinousJobTracker {

  static final Duration defaultTrackingInterval = Duration.ofSeconds(10);

  private final String apiUrl;
  private final Duration trackingInterval;
  private Thread trackingThread;
  private Optional<Checkpoints> latestCheckpoints = Optional.empty();

  public OpenLineageContinousJobTracker(String uri) {
    this(uri, defaultTrackingInterval);
  }

  public OpenLineageContinousJobTracker(String apiUrl, Duration trackingInterval) {
    this.apiUrl = apiUrl;
    this.trackingInterval = trackingInterval;
  }

  // TODO: write some test to this class

  /** Starts tracking flink JOB rest API */
  public void startTracking(FlinkExecutionContext context) {
    CloseableHttpClient httpClient = HttpClients.createDefault();

    String checkpointApiUrl =
        String.format("%s/jobs/%s/checkpoints", apiUrl, context.getJobId().toString());
    HttpGet request = new HttpGet(checkpointApiUrl);

    trackingThread =
        (new Thread(
            () -> {
              boolean shouldContinue = true;
              while (shouldContinue) {
                try {
                  Thread.sleep(trackingInterval.toMillis());

                  CloseableHttpResponse response = httpClient.execute(request);
                  String json = EntityUtils.toString(response.getEntity());
                  log.info(json);

                  Optional.of(
                          new ObjectMapper()
                              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                              .readValue(json, Checkpoints.class))
                      .filter(
                          newCheckpoints ->
                              latestCheckpoints.isEmpty()
                                  || latestCheckpoints.get().getCounts().getTotal()
                                      == newCheckpoints.getCounts().getTotal())
                      .ifPresentOrElse(
                          newCheckpoints -> emitNewCheckpointEvent(context, newCheckpoints),
                          () -> log.info("no new checkpoint found"));
                  shouldContinue = response.getCode() == 200;
                } catch (IOException | ParseException e) {
                  shouldContinue = false;
                  log.info("Connecting REST API failed", e);
                } catch (InterruptedException e) {
                  shouldContinue = false;
                  log.error("Stopping ");
                } catch (Exception e) {
                  shouldContinue = false;
                  log.error("tracker thread failed", e);
                }
              }
              log.info("Tracking got finished");
            }));
    trackingThread.start();
  }

  private void emitNewCheckpointEvent(FlinkExecutionContext context, Checkpoints newCheckpoints) {
    log.info(
        "New checkpoint encountered total-checkpoint:{}", newCheckpoints.getCounts().getTotal());
    latestCheckpoints = Optional.of(newCheckpoints);
    context.onJobCheckpoint(); // TODO: add some checkpointing information to the event
  }

  /** Stops the tracking thread */
  public void stopTracking() {
    log.info("stop tracking");
    trackingThread.interrupt();
  }
}
