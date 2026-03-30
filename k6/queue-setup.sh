#!/bin/bash
# =============================================================================
# v2 Queue 공정성 테스트 — 셋업 + 실행 스크립트
#
# 전제조건:
#   1. Spring Boot 앱이 실행 중 (local 프로파일, port 8080)
#      → ./gradlew bootRun 또는 IDE에서 KopangApplication 실행
#   2. Redis가 실행 중 (localhost:6379)
#   3. H2 콘솔에서 seed.sql을 이미 실행한 상태
#      → http://localhost:8080/h2-console
#         JDBC URL: jdbc:h2:mem:kopang / User: sa / Password: (빈칸)
#   4. k6 설치됨    → brew install k6
#   5. redis-cli 설치됨 → brew install redis
#   6. Python 3.8+ 설치됨
#
# 실행:
#   cd k6
#   bash queue-setup.sh
#
# 환경 변수 오버라이드:
#   BASE_URL=http://myhost:8080 EVENT_ID=2 bash queue-setup.sh
# =============================================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-1}"
PRODUCT_NO="${PRODUCT_NO:-1}"
MEMBER_NO="${MEMBER_NO:-1}"
WAREHOUSE_NO="${WAREHOUSE_NO:-1}"

STOCK_KEY="stock:product:${PRODUCT_NO}:warehouse:${WAREHOUSE_NO}"
QUEUE_EVENT_KEY="queue:event:${EVENT_ID}"
QUEUE_ACTIVE_EVENTS="queue:active_events"
QUEUE_ACTIVE_KEY="queue:active:${EVENT_ID}"
QUEUE_LOCK_KEY="queue:lock:${EVENT_ID}"
MEMBER_CACHE_KEY="member_address::${MEMBER_NO}"

VU_COUNT=800
OUTPUT_FILE="k6_v2_output.txt"

# ─── 색상 출력 ──────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
step()    { echo -e "\n${CYAN}${BOLD}$1${NC}"; }

# ─── 전제조건 체크 ──────────────────────────────────────────────────────────
step "[ 전제조건 확인 ]"

command -v k6        >/dev/null 2>&1 || error "k6 미설치. brew install k6"
command -v redis-cli >/dev/null 2>&1 || error "redis-cli 미설치. brew install redis"
command -v python3   >/dev/null 2>&1 || error "python3 미설치"

# 앱 헬스체크
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  "${BASE_URL}/actuator/health" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" != "200" ]; then
  warn "앱이 응답하지 않습니다 (status=${HTTP_STATUS})."
  warn "KopangApplication을 local 프로파일로 먼저 실행하세요."
  warn "계속하시겠습니까? [y/N]"
  read -r yn; [[ "$yn" =~ ^[Yy]$ ]] || exit 1
else
  info "앱 정상 응답 (${BASE_URL})"
fi

# Redis 연결 체크
redis-cli -u redis://localhost:6379 PING >/dev/null 2>&1 || \
  error "Redis 연결 실패 (localhost:6379)"
info "Redis 정상 연결"

# ─── STEP 1: Redis 초기화 ───────────────────────────────────────────────────
step "[ STEP 1/5 | Redis 초기화 ]"

# 이전 테스트 잔여 대기열 키 정리
redis-cli DEL \
  "$QUEUE_EVENT_KEY" \
  "$QUEUE_ACTIVE_EVENTS" \
  "$QUEUE_ACTIVE_KEY" \
  "$QUEUE_LOCK_KEY" \
  "$MEMBER_CACHE_KEY" \
  >/dev/null

# queue:entry:* 패턴 삭제
ENTRY_KEYS=$(redis-cli KEYS "queue:entry:*" | wc -l | tr -d ' ')
if [ "$ENTRY_KEYS" -gt "0" ]; then
  redis-cli KEYS "queue:entry:*" | xargs redis-cli DEL >/dev/null
  info "  queue:entry:* ${ENTRY_KEYS}개 삭제"
fi

# queue:member:{eventId}:* 패턴 삭제 (중복 대기열 진입 방지 키)
MEMBER_KEYS=$(redis-cli KEYS "queue:member:${EVENT_ID}:*" | wc -l | tr -d ' ')
if [ "$MEMBER_KEYS" -gt "0" ]; then
  redis-cli KEYS "queue:member:${EVENT_ID}:*" | xargs redis-cli DEL >/dev/null
  info "  queue:member:${EVENT_ID}:* ${MEMBER_KEYS}개 삭제"
fi

# Redis 재고 설정 (VU 수 + 워밍업 1개)
INITIAL_STOCK=$((VU_COUNT + 1))
redis-cli SET "$STOCK_KEY" "$INITIAL_STOCK" >/dev/null
info "  ${STOCK_KEY} = $(redis-cli GET $STOCK_KEY)  (VU ${VU_COUNT}개 + 워밍업 1개)"

# ─── STEP 2: Caffeine 캐시 워밍업 ───────────────────────────────────────────
# warehouse/product 캐시가 cold 상태면 첫 요청에 DB hit → 가변 지연 발생
# 워밍업 요청 1개로 캐시를 warm 상태로 만든 뒤 테스트 진행
step "[ STEP 2/5 | Caffeine 캐시 워밍업 (X-Queue-Token 없는 상시 주문) ]"

WARM_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/orders?memberNo=${MEMBER_NO}" \
  -H "Content-Type: application/json" \
  -d "{\"productNo\": ${PRODUCT_NO}, \"count\": 1}" 2>/dev/null || echo "000")

if [ "$WARM_STATUS" = "201" ]; then
  info "  워밍업 완료 (201 Created)"
  info "  남은 재고: ${STOCK_KEY} = $(redis-cli GET $STOCK_KEY)  (예상: ${VU_COUNT})"
else
  warn "  워밍업 응답: ${WARM_STATUS}"
  warn "  seed.sql이 실행됐는지 확인하세요: http://localhost:8080/h2-console"
  warn "  계속하시겠습니까? [y/N]"
  read -r yn; [[ "$yn" =~ ^[Yy]$ ]] || exit 1
fi

# ─── STEP 3: 테스트 정보 출력 ───────────────────────────────────────────────
step "[ STEP 3/5 | 테스트 구성 ]"
echo ""
echo "  대상 서버  : ${BASE_URL}"
echo "  eventId    : ${EVENT_ID}  (= productNo ${PRODUCT_NO})"
echo "  memberNo   : ${MEMBER_NO}"
echo "  VU 수      : ${VU_COUNT}"
echo "  재고       : $(redis-cli GET $STOCK_KEY)"
echo ""
echo "  흐름:"
echo "    Phase 1  POST /api/v2/events/${EVENT_ID}/queue?memberNo=${MEMBER_NO}"
echo "             → 202 Accepted { token, position, estimatedWaitMs }"
echo "    Phase 2  GET  /api/v2/events/${EVENT_ID}/queue/{token}/status  (0.5s 간격 폴링)"
echo "             → ACTIVE 상태 대기 (워커가 FIFO 순서로 배치 활성화)"
echo "    Phase 3  POST /api/v1/orders?memberNo=${MEMBER_NO}  + X-Queue-Token: {token}"
echo "             → 201 Created { orderNo }"
echo ""
echo "  공정성 판정 기준:"
echo "    position_i < position_j  이면서  orderNo_i > orderNo_j  → 역전(공정성 위반)"
echo "    배치 간 역전율 = 0%  →  ✅ v2 FIFO 공정성 보장"
echo ""

# ─── STEP 4: k6 실행 ────────────────────────────────────────────────────────
step "[ STEP 4/5 | k6 v2 공정성 테스트 실행 ]"
info "  출력 파일: ${OUTPUT_FILE}"
echo ""

k6 run \
  --env BASE_URL="${BASE_URL}" \
  --env EVENT_ID="${EVENT_ID}" \
  --env PRODUCT_NO="${PRODUCT_NO}" \
  --env MEMBER_NO="${MEMBER_NO}" \
  queue-fairness-test.js > "$OUTPUT_FILE" 2>&1

# 결과 집계
RESULT_COUNT=$(grep -c '\[V2RESULT\]' "$OUTPUT_FILE" 2>/dev/null || echo 0)
ERROR_COUNT=$(grep -c '\[V2ERROR\]' "$OUTPUT_FILE" 2>/dev/null || echo 0)
REMAINING_STOCK=$(redis-cli GET "$STOCK_KEY" || echo "알 수 없음")

echo ""
info "  성공: ${RESULT_COUNT}건 / 에러: ${ERROR_COUNT}건"
info "  남은 Redis 재고: ${REMAINING_STOCK}  (예상: 0)"

# ─── STEP 5: 공정성 분석 ─────────────────────────────────────────────────────
step "[ STEP 5/5 | 공정성 분석 ]"
echo ""

python3 queue-analyze.py "$OUTPUT_FILE"

echo ""
info "완료."
echo ""
echo "  상세 k6 출력  : cat ${OUTPUT_FILE}"
echo "  재분석        : python3 queue-analyze.py ${OUTPUT_FILE}"
echo "  v1 비교 실행  : bash setup.sh  (v1 Lua Script 공정성 재측정)"
