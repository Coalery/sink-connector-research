#!/bin/bash

set -e

echo "=== Redis Heartbeat Sink Connector 설정 시작 ==="

# Kafka Connect가 시작될 때까지 대기
echo "Kafka Connect 서비스 시작 대기 중..."
while ! curl -f http://kafka-connect:8083/; do
    echo "Kafka Connect 아직 준비되지 않음... 5초 대기"
    sleep 5
done

echo "Kafka Connect 서비스 준비 완료!"

# 토픽 생성
echo "heartbeat-topic 토픽 생성 중..."
kafka-topics --create \
    --topic heartbeat-topic \
    --bootstrap-server kafka:29092 \
    --partitions 1 \
    --replication-factor 1 \
    --if-not-exists

echo "토픽 생성 완료!"

# Connector 등록
echo "Redis Heartbeat Sink Connector 등록 중..."
curl -X POST \
    -H "Content-Type: application/json" \
    -d @/config/redis-heartbeat-sink.json \
    http://kafka-connect:8083/connectors

echo ""
echo "Connector 등록 완료!"

# Connector 상태 확인
echo "Connector 상태 확인 중..."
sleep 5
curl -X GET http://kafka-connect:8083/connectors/redis-heartbeat-sink/status

echo ""
echo "=== 설정 완료 ==="
