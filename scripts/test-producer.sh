#!/bin/bash

set -e

echo "=== Kafka 메시지 생성 테스트 시작 ==="

TOPIC="heartbeat-topic"
BOOTSTRAP_SERVER="kafka:29092"

echo "토픽 '$TOPIC'에 테스트 메시지 전송 중..."

# 무한 루프로 10초마다 메시지 전송
counter=1
while true; do
    message="heartbeat-$counter-$(date '+%Y-%m-%d %H:%M:%S')"
    echo "메시지 #$counter 전송: $message"
    
    echo "$message" | kafka-console-producer \
        --topic $TOPIC \
        --bootstrap-server $BOOTSTRAP_SERVER
    
    echo "메시지 전송 완료! 10초 대기..."
    sleep 10
    counter=$((counter + 1))
done
