package com.example;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.Collection;
import java.util.Map;
import java.time.Duration;

public class RedisHeartbeatSinkTask extends SinkTask {
  private static final Logger log = LoggerFactory.getLogger(RedisHeartbeatSinkTask.class);

  private RedisHeartbeatSinkConnectorConfig config;
  private RedisClient redisClient;
  private StatefulRedisConnection<String, String> connection;
  private String redisKey;
  private int redisTtl;

  @Override
  public String version() {
    return "1.0.0";
  }

  @Override
  public void start(Map<String, String> props) {
    log.info("Starting Redis Heartbeat Sink Task");
    config = new RedisHeartbeatSinkConnectorConfig(props);

    // Redis 연결 설정
    String redisHost = config.getRedisHost();
    int redisPort = config.getRedisPort();
    String redisPassword = config.getRedisPassword();
    int redisDatabase = config.getRedisDatabase();
    redisKey = config.getRedisKey();
    redisTtl = config.getRedisTtl();

    log.info("Connecting to Redis at {}:{} with key: {}, TTL: {} seconds",
        redisHost, redisPort, redisKey, redisTtl);

    // Redis URI 구성
    RedisURI.Builder builder = RedisURI.builder()
        .withHost(redisHost)
        .withPort(redisPort)
        .withDatabase(redisDatabase)
        .withTimeout(Duration.ofMillis(2000));

    if (redisPassword != null && !redisPassword.isEmpty()) {
      builder.withPassword(redisPassword.toCharArray());
    }

    RedisURI redisUri = builder.build();
    redisClient = RedisClient.create(redisUri);
    connection = redisClient.connect();

    // 초기 heartbeat 키 설정
    try {
      RedisCommands<String, String> commands = connection.sync();
      commands.setex(redisKey, redisTtl, String.valueOf(System.currentTimeMillis()));
      log.info("Initial heartbeat key '{}' set with TTL {} seconds", redisKey, redisTtl);
    } catch (Exception e) {
      log.error("Failed to initialize Redis heartbeat key", e);
      throw new RuntimeException("Failed to initialize Redis connection", e);
    }
  }

  @Override
  public void put(Collection<SinkRecord> records) {
    if (records.isEmpty()) {
      return;
    }

    log.debug("Processing {} records", records.size());

    try {
      RedisCommands<String, String> commands = connection.sync();
      for (SinkRecord record : records) {
        // 각 메시지마다 heartbeat 키의 TTL을 갱신
        long currentTime = System.currentTimeMillis();
        String value = String.valueOf(currentTime);

        // SETEX 명령으로 값과 TTL을 동시에 설정
        commands.setex(redisKey, redisTtl, value);

        log.debug(
            "Updated heartbeat key '{}' with value '{}' and TTL {} seconds for record from topic: {}, partition: {}, offset: {}",
            redisKey, value, redisTtl, record.topic(), record.kafkaPartition(), record.kafkaOffset());
      }

      log.info("Successfully processed {} records and updated Redis heartbeat", records.size());

    } catch (Exception e) {
      log.error("Failed to update Redis heartbeat", e);
      throw new RuntimeException("Failed to update Redis heartbeat", e);
    }
  }

  @Override
  public void stop() {
    log.info("Stopping Redis Heartbeat Sink Task");

    if (connection != null && connection.isOpen()) {
      connection.close();
    }
    if (redisClient != null) {
      redisClient.shutdown();
    }
    log.info("Redis connection closed");
  }
}
