#!/bin/bash
# =============================================================================
# fairness-test 셋업 + 실행 스크립트
#
# 전제조건:
#   1. Spring Boot 앱이 실행 중 (local 프로파일, port 8080)
#   2. Redis가 실행 중 (localhost:6379)
#   3. H2 콘솔에서 seed.sql을 이미 실행한 상태
#      → http://localhost:8080/h2-console
#         JDBC URL: jdbc:h2:mem:kopang / User: sa
#   4. k6 설치됨 (brew install k6)
#   5. Python 3.8+ 설치됨
# =============================================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
STOCK_KEY="stock:product:1:warehouse:1"
VU_COUNT=50
OUTPUT_FILE="k6_output.txt"
RESULTS_FILE="results.txt"

# ─── 색상 출력 ─────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ─── 전제조건 체크 ─────────────────────────────────────────────────────────
info "전제조건 확인 중..."

command -v k6      >/dev/null 2>&1 || error "k6 미설치. brew install k6"
command -v redis-cli >/dev/null 2>&1 || error "redis-cli 미설치"
command -v python3 >/dev/null 2>&1 || error "python3 미설치"

# 앱 헬스체크
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" != "200" ]; then
  warn "앱이 응답하지 않습니다 (status=${HTTP_STATUS}). 계속하시겠습니까? [y/N]"
  read -r yn
  [[ "$yn" =~ ^[Yy]$ ]] || exit 1
fi

# Redis 연결 체크
redis-cli PING >/dev/null 2>&1 || error "Redis 연결 실패 (localhost:6379)"

echo ""

# ─── STEP 1: Caffeine 캐시 워밍업 ────────────────────────────────────────
# warehouse, product → Caffeine 캐시 (프로세스 재시작 시 cold)
# 워밍업 없이 50 VU가 동시에 첫 요청을 보내면 모두 DB hit → 가변 지연 발생
# 이 가변 지연은 역전 효과를 더 크게 만들기도 하지만, 순수한 측정을 위해 워밍업 수행
#
# 주의: 워밍업 요청 1개가 재고를 1 소모. 그래서 Redis를 VU_COUNT+1로 설정.

INITIAL_STOCK=$((VU_COUNT + 1))

info "STEP 1/4 | Redis 재고 설정 → ${INITIAL_STOCK} (VU ${VU_COUNT}개 + 워밍업 1개)"
redis-cli SET "$STOCK_KEY" "$INITIAL_STOCK" > /dev/null
echo "  Redis ${STOCK_KEY} = $(redis-cli GET $STOCK_KEY)"

echo ""
info "STEP 2/4 | Caffeine 캐시 워밍업 (단일 요청)"
WARM_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/orders?memberNo=1" \
  -H "Content-Type: application/json" \
  -d '{"productNo": 1, "count": 1}')

if [ "$WARM_RESPONSE" = "201" ]; then
  info "  워밍업 완료 (201 Created). 남은 재고: $(redis-cli GET $STOCK_KEY)"
else
  error "  워밍업 실패 (status=${WARM_RESPONSE}). seed.sql이 실행됐는지 확인하세요."
fi

echo ""
info "STEP 3/4 | k6 공정성 테스트 실행 (VU=${VU_COUNT}, 1회/VU)"
echo "  출력 파일: ${OUTPUT_FILE}"
echo ""

k6 run \
  --env BASE_URL="${BASE_URL}" \
  fairness-test.js > "$OUTPUT_FILE" 2>&1

# 결과 줄만 추출
grep '\[RESULT\]' "$OUTPUT_FILE" > "$RESULTS_FILE"
RESULT_COUNT=$(wc -l < "$RESULTS_FILE" | tr -d ' ')
ERROR_COUNT=$(grep -c '\[ERROR\]' "$OUTPUT_FILE" || true)

info "  성공: ${RESULT_COUNT}건  에러: ${ERROR_COUNT}건"

REMAINING=$(redis-cli GET "$STOCK_KEY" || echo "0")
info "  Redis 남은 재고: ${REMAINING:-0} (예상: 0)"

echo ""
info "STEP 4/4 | 공정성 분석"
echo ""
python3 analyze.py "$OUTPUT_FILE"

echo ""
info "완료. 상세 k6 출력: cat ${OUTPUT_FILE}"