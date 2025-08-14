package com.example;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedisHeartbeatSinkConnector extends SinkConnector {
  private static final Logger log = LoggerFactory.getLogger(RedisHeartbeatSinkConnector.class);

  private Map<String, String> configProps;

  @Override
  public String version() {
    return "1.0.0";
  }

  @Override
  public void start(Map<String, String> props) {
    log.info("Starting Redis Heartbeat Sink Connector");
    this.configProps = props;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return RedisHeartbeatSinkTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    log.info("Creating {} task configurations", maxTasks);
    List<Map<String, String>> configs = new ArrayList<>();
    for (int i = 0; i < maxTasks; i++) {
      configs.add(configProps);
    }
    return configs;
  }

  @Override
  public void stop() {
    log.info("Stopping Redis Heartbeat Sink Connector");
  }

  @Override
  public ConfigDef config() {
    return RedisHeartbeatSinkConnectorConfig.configDef();
  }
}
