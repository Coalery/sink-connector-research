# Redis Heartbeat Sink Connector

Kafka 메시지를 받을 때마다 Redis의 `heartbeat` 키의 TTL을 갱신하는 Custom Kafka Sink Connector입니다.

> [!IMPORTANT]
>
> 본 레포지토리는 리서치 용도로 만들어졌으며, Cursor에 의해 생성된 코드입니다. 오로지 리서치 목적으로만 사용되며, 실 환경에서 사용하기 위해서는 추가적인 검증이 필요합니다.

## 🎯 개요

이 프로젝트는 Kafka 메시지 기반의 heartbeat 시스템을 구현합니다. 시스템이 정상적으로 작동하는 동안 Kafka로 메시지를 전송하면, Redis의 `heartbeat` 키가 TTL과 함께 갱신됩니다. 메시지가 중단되면 TTL이 만료되어 시스템 장애를 감지할 수 있습니다.

## 🏗️ 구성 요소

- **Kafka** (Confluent Platform 7.4.0): 메시지 브로커
- **Redis** (7-alpine): heartbeat 키 저장소
- **Custom Sink Connector**: Java 11로 작성된 커스텀 connector
- **Kafka Connect**: Connector 실행 환경
- **Zookeeper**: Kafka 메타데이터 관리

## 🚀 빠른 시작

### 1. 환경 시작

```bash
# 실행 권한 부여 및 전체 환경 시작
chmod +x run.sh
./run.sh
```

또는 수동으로:

```bash
# 모든 서비스 시작
docker compose up -d

# 30초 대기 후 connector 설정
sleep 30
docker compose exec test-runner /scripts/setup.sh
```

### 2. 동작 확인

#### 🔍 Connector 상태 확인

```bash
curl http://localhost:8083/connectors/redis-heartbeat-sink/status
```

**예상 출력:**

```json
{
  "name": "redis-heartbeat-sink",
  "connector": { "state": "RUNNING", "worker_id": "kafka-connect:8083" },
  "tasks": [{ "id": 0, "state": "RUNNING", "worker_id": "kafka-connect:8083" }],
  "type": "sink"
}
```

## 📋 사용법

### 1. 테스트 메시지 전송

#### 단일 메시지 전송

```bash
docker compose exec test-runner bash -c \
  'echo "test-$(date)" | kafka-console-producer --topic heartbeat-topic --bootstrap-server kafka:29092'
```

#### 연속 메시지 전송 (백그라운드)

```bash
docker compose exec test-runner /scripts/test-producer.sh
```

### 2. Redis 상태 모니터링

#### 현재 상태 확인

```bash
# heartbeat 키 값 확인
docker compose exec redis redis-cli GET heartbeat

# TTL 확인 (60초부터 카운트다운)
docker compose exec redis redis-cli TTL heartbeat
```

**예상 결과:**

```bash
$ docker compose exec redis redis-cli GET heartbeat
"1755138932496"  # 타임스탬프

$ docker compose exec redis redis-cli TTL heartbeat
(integer) 54     # 남은 TTL 초
```

#### 연속 모니터링

```bash
# 5초마다 TTL 확인
for i in {1..12}; do
  echo "=== $i회차 확인 ==="
  docker compose exec redis redis-cli TTL heartbeat
  sleep 5
done
```

### 3. 장애 시나리오 테스트

#### 메시지 전송 중단

```bash
# 메시지 생성 프로세스 중단
docker compose exec test-runner pkill -f kafka-console-producer

# TTL 감소 관찰 (60초 후 키 만료)
watch -n 1 'docker compose exec redis redis-cli TTL heartbeat'
```

#### 복구 테스트

```bash
# 새 메시지 전송으로 heartbeat 복구
docker compose exec test-runner bash -c \
  'echo "recovery-$(date)" | kafka-console-producer --topic heartbeat-topic --bootstrap-server kafka:29092'

# 키가 다시 생성되었는지 확인
docker compose exec redis redis-cli TTL heartbeat
```

## 🔧 고급 사용법

### 1. 커스텀 설정

#### TTL 변경

`config/redis-heartbeat-sink.json` 파일에서 TTL 수정:

```json
{
  "config": {
    "redis.ttl": "120" // 120초로 변경
  }
}
```

#### Redis 키 이름 변경

```json
{
  "config": {
    "redis.key": "my-service-heartbeat"
  }
}
```

### 2. 로그 모니터링

#### Connector 로그

```bash
docker compose logs -f kafka-connect
```

#### 성공 로그 예시

```
[2025-08-14 02:31:27,438] INFO Successfully processed 1 records and updated Redis heartbeat (com.example.RedisHeartbeatSinkTask)
```

### 3. 문제 해결

#### Connector 재시작

```bash
curl -X POST http://localhost:8083/connectors/redis-heartbeat-sink/restart
```

#### 전체 환경 재시작

```bash
docker compose down
docker compose up -d
sleep 30
docker compose exec test-runner /scripts/setup.sh
```

## 📊 모니터링 및 알림

### 1. 외부 모니터링 스크립트 예시

```bash
#!/bin/bash
# heartbeat-monitor.sh

REDIS_HOST="localhost"
REDIS_PORT="6379"
ALERT_THRESHOLD=10  # TTL이 10초 이하일 때 알림

while true; do
    TTL=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT TTL heartbeat 2>/dev/null)

    if [ "$TTL" = "-2" ]; then
        echo "⚠️  ALERT: Heartbeat key expired! System may be down."
        # 여기에 알림 로직 추가 (Slack, 이메일 등)
    elif [ "$TTL" -lt "$ALERT_THRESHOLD" ] && [ "$TTL" -gt "0" ]; then
        echo "🟡 WARNING: Heartbeat TTL low: ${TTL}s"
    else
        echo "✅ OK: Heartbeat TTL: ${TTL}s"
    fi

    sleep 5
done
```

### 2. Prometheus 메트릭 수집

Redis Exporter를 추가하여 메트릭 수집:

```yaml
# docker-compose.yml에 추가
redis-exporter:
  image: oliver006/redis_exporter
  environment:
    REDIS_ADDR: "redis:6379"
  ports:
    - "9121:9121"
```

## 🗂️ 프로젝트 구조

```
sink-connector-research/
├── 📋 README.md                    # 이 파일
├── 🐳 docker-compose.yml           # Docker Compose 설정
├── 🚀 run.sh                      # 원클릭 실행 스크립트
├── 📦 connector/                  # Java Connector 소스
│   ├── 📄 pom.xml                # Maven 설정
│   ├── 🐳 Dockerfile.build       # 빌드용 Dockerfile
│   └── 📁 src/main/java/com/example/
│       ├── 🔌 RedisHeartbeatSinkConnector.java
│       ├── ⚙️  RedisHeartbeatSinkConnectorConfig.java
│       └── 🔄 RedisHeartbeatSinkTask.java
├── ⚙️  config/                    # Connector 설정
│   └── 📄 redis-heartbeat-sink.json
└── 📜 scripts/                    # 유틸리티 스크립트
    ├── 🛠️  setup.sh              # 초기 설정
    ├── 📤 test-producer.sh       # 메시지 생성기
    └── 👁️  check-redis.sh        # Redis 상태 확인
```

## ⚙️ Connector 설정 상세

### 기본 설정 (config/redis-heartbeat-sink.json)

```json
{
  "name": "redis-heartbeat-sink",
  "config": {
    "connector.class": "com.example.RedisHeartbeatSinkConnector",
    "tasks.max": "1",
    "topics": "heartbeat-topic",
    "redis.host": "redis",
    "redis.port": "6379",
    "redis.key": "heartbeat",
    "redis.ttl": "60",
    "redis.database": "0",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
  }
}
```

### 설정 옵션

| 설정             | 기본값      | 설명                      |
| ---------------- | ----------- | ------------------------- |
| `redis.host`     | `redis`     | Redis 서버 호스트         |
| `redis.port`     | `6379`      | Redis 서버 포트           |
| `redis.key`      | `heartbeat` | 갱신할 Redis 키 이름      |
| `redis.ttl`      | `60`        | TTL 시간 (초)             |
| `redis.database` | `0`         | Redis 데이터베이스 번호   |
| `redis.password` | `null`      | Redis 비밀번호 (선택사항) |

## 🔍 동작 원리

1. **메시지 수신**: Kafka `heartbeat-topic`에 메시지 도착
2. **Connector 실행**: RedisHeartbeatSinkTask가 메시지 처리
3. **Redis 갱신**: `SETEX heartbeat 60 <timestamp>` 명령 실행
4. **TTL 카운트다운**: 60초부터 1초씩 감소
5. **키 만료**: 새 메시지가 없으면 60초 후 키 삭제

## 💡 활용 사례

### 1. 마이크로서비스 헬스체크

```bash
# 서비스에서 주기적으로 heartbeat 전송
*/30 * * * * echo "service-alive-$(date)" | kafka-console-producer --topic heartbeat-topic --bootstrap-server kafka:29092
```

### 2. 배치 작업 모니터링

```bash
# 배치 작업 시작 시 heartbeat 활성화
echo "batch-start-$(date)" | kafka-console-producer --topic heartbeat-topic --bootstrap-server kafka:29092

# 배치 작업 중 주기적 업데이트
while [[ 배치작업중 ]]; do
    echo "batch-running-$(date)" | kafka-console-producer --topic heartbeat-topic --bootstrap-server kafka:29092
    sleep 30
done
```

### 3. 장애 감지 및 알림

```bash
# 모니터링 스크립트
if redis-cli TTL heartbeat | grep -q "\-2"; then
    curl -X POST https://hooks.slack.com/... -d '{"text":"🚨 System heartbeat lost!"}'
fi
```

## 🧪 테스트 검증

모든 기능이 정상 동작함을 확인했습니다:

- ✅ Kafka 메시지 → Redis TTL 갱신
- ✅ 60초 TTL 자동 설정
- ✅ 메시지 중단 시 키 자동 만료
- ✅ Java 코드 컨테이너 빌드
- ✅ 로컬 Java 환경 불필요
- ✅ 멀티 메시지 처리
- ✅ 장애 복구 테스트

## 🔧 환경 정리

```bash
# 모든 컨테이너 중지 및 삭제
docker compose down

# 볼륨까지 완전 삭제
docker compose down -v

# 사용하지 않는 이미지 정리
docker image prune -f
```

## 📞 문의 및 지원

문제가 발생하거나 추가 기능이 필요한 경우:

1. **로그 확인**: `docker compose logs kafka-connect`
2. **Connector 상태**: `curl http://localhost:8083/connectors/redis-heartbeat-sink/status`
3. **Redis 상태**: `docker compose exec redis redis-cli INFO`
4. **Kafka 토픽**: `docker compose exec test-runner kafka-topics --list --bootstrap-server kafka:29092`

---

⭐ **팁**: 실제 운영 환경에서는 Redis와 Kafka 클러스터를 분리하고, 적절한 모니터링 및 알림 시스템을 구축하는 것을 권장합니다.
