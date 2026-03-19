/**
 * 공정성(Fairness) 검증 테스트
 *
 * 목적:
 *   Lua Script는 over-selling을 막지만, "먼저 요청한 사람이 먼저 처리된다"는
 *   FIFO 공정성을 보장하지 않는다는 사실을 실측 데이터로 증명한다.
 *
 * 측정 방식:
 *   - VU=50이 동시에 1번씩 POST /api/v1/orders 요청
 *   - 각 요청의 send_time (HTTP 발송 직전 타임스탬프) 기록
 *   - 응답에서 orderNo (DB auto-increment) 기록
 *   - 분석: send_time 순서 vs orderNo 순서의 불일치(역전) 계산
 *
 * 전제:
 *   - orderNo는 DB INSERT 순서에 따라 자동 증가 → 높은 orderNo = 늦게 처리됨
 *   - send_time이 작을수록 먼저 보낸 요청
 *   - 공정성이 보장된다면: 작은 send_time → 작은 orderNo (단조 증가)
 *   - 위반: 작은 send_time → 큰 orderNo (역전)
 *
 * 실행 방법:
 *   1. setup.sh 실행 (또는 README의 수동 설정 참고)
 *   2. k6 run fairness-test.js > k6_output.txt 2>&1
 *   3. python3 analyze.py k6_output.txt
 */

import http from 'k6/http';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_NO = __ENV.PRODUCT_NO || 1;
const MEMBER_NO = __ENV.MEMBER_NO || 1;

export const options = {
  scenarios: {
    fairness_check: {
      // per-vu-iterations: 각 VU가 정확히 1번만 실행
      // → VU 50개가 동시에 1개 요청씩 = 총 50 요청
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  // 공정성 테스트는 성공률 thresholds 불필요 — 분석이 목적
  thresholds: {},
};

export default function () {
  const vuId = exec.vu.idInTest; // 1~50 (VU 번호, 시작 순서 아님)

  // HTTP 발송 직전 타임스탬프 → "보낸 순서"의 proxy
  const sendTime = Date.now();

  const res = http.post(
    `${BASE_URL}/api/v1/orders?memberNo=${MEMBER_NO}`,
    JSON.stringify({ productNo: Number(PRODUCT_NO), count: 1 }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'fairness_order' },
    }
  );

  const receiveTime = Date.now();
  const latencyMs = receiveTime - sendTime;

  if (res.status === 201) {
    const orderNo = res.json('orderNo');
    // analyze.py가 파싱하는 구조화된 로그
    console.log(
      `[RESULT] vu=${vuId} send_time=${sendTime} latency_ms=${latencyMs} order_no=${orderNo}`
    );
  } else {
    // 409(품절), 500(에러) 등 비정상 응답
    console.log(
      `[ERROR] vu=${vuId} send_time=${sendTime} status=${res.status} body=${res.body}`
    );
  }
}
