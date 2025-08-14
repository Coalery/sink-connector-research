package com.example;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

import java.util.Map;

public class RedisHeartbeatSinkConnectorConfig extends AbstractConfig {

  public static final String REDIS_HOST_CONFIG = "redis.host";
  public static final String REDIS_HOST_DOC = "Redis server hostname";
  public static final String REDIS_HOST_DEFAULT = "localhost";

  public static final String REDIS_PORT_CONFIG = "redis.port";
  public static final String REDIS_PORT_DOC = "Redis server port";
  public static final int REDIS_PORT_DEFAULT = 6379;

  public static final String REDIS_KEY_CONFIG = "redis.key";
  public static final String REDIS_KEY_DOC = "Redis key to update TTL";
  public static final String REDIS_KEY_DEFAULT = "heartbeat";

  public static final String REDIS_TTL_CONFIG = "redis.ttl";
  public static final String REDIS_TTL_DOC = "TTL in seconds for the Redis key";
  public static final int REDIS_TTL_DEFAULT = 60;

  public static final String REDIS_PASSWORD_CONFIG = "redis.password";
  public static final String REDIS_PASSWORD_DOC = "Redis password (optional)";
  public static final String REDIS_PASSWORD_DEFAULT = null;

  public static final String REDIS_DATABASE_CONFIG = "redis.database";
  public static final String REDIS_DATABASE_DOC = "Redis database number";
  public static final int REDIS_DATABASE_DEFAULT = 0;

  public RedisHeartbeatSinkConnectorConfig(Map<?, ?> props) {
    super(configDef(), props);
  }

  public static ConfigDef configDef() {
    return new ConfigDef()
        .define(REDIS_HOST_CONFIG, Type.STRING, REDIS_HOST_DEFAULT, Importance.HIGH, REDIS_HOST_DOC)
        .define(REDIS_PORT_CONFIG, Type.INT, REDIS_PORT_DEFAULT, Importance.HIGH, REDIS_PORT_DOC)
        .define(REDIS_KEY_CONFIG, Type.STRING, REDIS_KEY_DEFAULT, Importance.HIGH, REDIS_KEY_DOC)
        .define(REDIS_TTL_CONFIG, Type.INT, REDIS_TTL_DEFAULT, Importance.HIGH, REDIS_TTL_DOC)
        .define(REDIS_PASSWORD_CONFIG, Type.PASSWORD, REDIS_PASSWORD_DEFAULT, Importance.LOW, REDIS_PASSWORD_DOC)
        .define(REDIS_DATABASE_CONFIG, Type.INT, REDIS_DATABASE_DEFAULT, Importance.LOW, REDIS_DATABASE_DOC);
  }

  public String getRedisHost() {
    return this.getString(REDIS_HOST_CONFIG);
  }

  public int getRedisPort() {
    return this.getInt(REDIS_PORT_CONFIG);
  }

  public String getRedisKey() {
    return this.getString(REDIS_KEY_CONFIG);
  }

  public int getRedisTtl() {
    return this.getInt(REDIS_TTL_CONFIG);
  }

  public String getRedisPassword() {
    return this.getPassword(REDIS_PASSWORD_CONFIG) != null ? this.getPassword(REDIS_PASSWORD_CONFIG).value() : null;
  }

  public int getRedisDatabase() {
    return this.getInt(REDIS_DATABASE_CONFIG);
  }
}
