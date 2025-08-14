#!/bin/bash

set -e

echo "=== Redis Heartbeat Sink Connector 환경 시작 ==="

# 스크립트에 실행 권한 부여
chmod +x scripts/*.sh

echo "1. Docker Compose로 모든 서비스 시작..."
docker compose up -d

echo ""
echo "2. 모든 서비스가 시작될 때까지 대기 중..."
sleep 30

echo ""
echo "3. Connector 설정 실행..."
docker compose exec test-runner /scripts/setup.sh

echo ""
echo "=== 환경 준비 완료! ==="
echo ""
echo "다음 명령어들로 테스트할 수 있습니다:"
echo ""
echo "# Kafka 메시지 생성 (별도 터미널에서 실행):"
echo "docker compose exec test-runner /scripts/test-producer.sh"
echo ""
echo "# Redis heartbeat 상태 확인 (별도 터미널에서 실행):"
echo "sh ./scripts/check-redis.sh"
echo ""
echo "# Connector 상태 확인:"
echo "curl http://localhost:8083/connectors/redis-heartbeat-sink/status"
echo ""
echo "# 로그 확인:"
echo "docker compose logs -f kafka-connect"
echo ""
echo "# 환경 종료:"
echo "docker compose down"
