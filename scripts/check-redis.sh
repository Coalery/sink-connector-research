#!/bin/bash

set -e

echo "=== Redis heartbeat 키 상태 확인 ==="

REDIS_KEY="heartbeat"

while true; do
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Redis heartbeat 키 상태:"
    
    # Redis에서 키 값과 TTL 확인 (docker exec를 통해)
    VALUE=$(docker exec redis redis-cli GET $REDIS_KEY 2>/dev/null || echo "키가 존재하지 않음")
    TTL=$(docker exec redis redis-cli TTL $REDIS_KEY 2>/dev/null || echo "-1")
    
    if [ "$VALUE" != "키가 존재하지 않음" ]; then
        echo "  키: $REDIS_KEY"
        echo "  값: $VALUE"
        echo "  TTL: $TTL 초"
        
        # 값을 타임스탬프로 해석하여 시간 표시
        if [[ "$VALUE" =~ ^[0-9]+$ ]]; then
            TIMESTAMP_DATE=$(date -r $(echo $VALUE | cut -c1-10) '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "Invalid timestamp")
            echo "  마지막 업데이트: $TIMESTAMP_DATE"
        fi
    else
        echo "  키 '$REDIS_KEY'가 존재하지 않습니다!"
    fi
    
    echo "----------------------------------------"
    sleep 5
done
