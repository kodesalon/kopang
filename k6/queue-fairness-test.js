/**
 * v2 Queue 공정성(Fairness) 검증 테스트
 *
 * 목적:
 *   Redis Sorted Set 대기열(FIFO) 아키텍처가 "먼저 진입한 사람이 먼저 주문된다"는
 *   선착순 공정성을 실측 데이터로 검증한다.
 *   v1(Lua Script)의 결과와 역전율을 비교한다.
 *
 * 측정 방식:
 *   Phase 1 — VU=50이 동시에 POST /api/v2/events/{eventId}/queue 진입
 *             → position(서버가 부여한 FIFO 순위) + token 수신
 *   Phase 2 — GET /api/v2/events/{eventId}/queue/{token}/status 폴링
 *             → ACTIVE 상태가 될 때까지 0.5초 간격으로 대기
 *   Phase 3 — POST /api/v1/orders + X-Queue-Token 헤더로 주문
 *             → orderNo(DB auto-increment) 기록
 *
 * 기대:
 *   v1 (Lua Script)  : send_time 순서 ≠ orderNo 순서 → 역전율 ~33~49%
 *   v2 (Queue)       : position 순서 = orderNo 순서 → 배치 간 역전율 ≈ 0%
 *                      (배치 내부 역전은 동일 배치의 동시 활성화로 허용)
 *
 * 로그 포맷 ([V2RESULT] 줄이 queue-analyze.py의 파싱 대상):
 *   [V2RESULT] vu={id} position={pos} entry_time={ms} activation_time={ms} wait_ms={ms} order_no={no}
 *
 * 실행 방법:
 *   1. queue-setup.sh 실행 (전제조건: 앱 실행, Redis, H2 seed.sql 완료)
 *   2. k6 run queue-fairness-test.js > k6_v2_output.txt 2>&1
 *   3. python3 queue-analyze.py k6_v2_output.txt
 */

import http from 'k6/http';
import { sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL  = __ENV.BASE_URL   || 'http://localhost:8080';
const EVENT_ID  = __ENV.EVENT_ID   || 1;
const MEMBER_NO = __ENV.MEMBER_NO  || 1;
const PRODUCT_NO = __ENV.PRODUCT_NO || 1;

const POLL_INTERVAL_SEC = 0.5;   // 워커 fixedDelay=500ms 에 맞춤
const MAX_POLL_ATTEMPTS = 60;    // 최대 30초 대기

export const options = {
  scenarios: {
    v2_fairness_check: {
      // per-vu-iterations: VU 50개가 각각 정확히 1번씩 실행
      // → 50개의 독립된 사용자가 동시에 대기열 진입
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '90s',
    },
  },
  // 공정성 분석이 목적 — 성능 threshold 불필요
  thresholds: {},
};

export default function () {
  const vuId = exec.vu.idInTest; // 1~50

  // ── Phase 1: 대기열 진입 ─────────────────────────────────────────────────
  const entryTime = Date.now();

  const enterRes = http.post(
    `${BASE_URL}/api/v2/events/${EVENT_ID}/queue?memberNo=${MEMBER_NO}`,
    JSON.stringify({ count: 1 }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'v2_queue_enter' },
    }
  );

  if (enterRes.status !== 202) {
    console.log(
      `[V2ERROR] vu=${vuId} phase=enter status=${enterRes.status} body=${enterRes.body}`
    );
    return;
  }

  const token    = enterRes.json('token');
  const position = enterRes.json('position');

  // ── Phase 2: ACTIVE 폴링 ─────────────────────────────────────────────────
  let status = 'WAITING';
  let activationTime = null;
  let polls = 0;

  while (status === 'WAITING' && polls < MAX_POLL_ATTEMPTS) {
    sleep(POLL_INTERVAL_SEC);

    const statusRes = http.get(
      `${BASE_URL}/api/v2/events/${EVENT_ID}/queue/${token}/status`,
      { tags: { name: 'v2_queue_poll' } }
    );

    if (statusRes.status === 200) {
      const s = statusRes.json('status');
      if (s === 'ACTIVE') {
        activationTime = Date.now();
        status = 'ACTIVE';
      } else if (s === 'EXPIRED') {
        status = 'EXPIRED';
      }
    }
    polls++;
  }

  if (status !== 'ACTIVE') {
    console.log(
      `[V2ERROR] vu=${vuId} phase=poll final_status=${status} position=${position} polls=${polls}`
    );
    return;
  }

  // ── Phase 3: 주문 (ACTIVE 토큰으로 v1 API 호출) ─────────────────────────
  const orderRes = http.post(
    `${BASE_URL}/api/v1/orders?memberNo=${MEMBER_NO}`,
    JSON.stringify({ productNo: Number(PRODUCT_NO), count: 1 }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Queue-Token': token,
      },
      tags: { name: 'v2_order' },
    }
  );

  if (orderRes.status !== 201) {
    console.log(
      `[V2ERROR] vu=${vuId} phase=order status=${orderRes.status} position=${position} body=${orderRes.body}`
    );
    return;
  }

  const orderNo = orderRes.json('orderNo');
  const waitMs  = activationTime - entryTime;

  // queue-analyze.py가 파싱하는 구조화된 로그
  console.log(
    `[V2RESULT] vu=${vuId} position=${position} entry_time=${entryTime}` +
    ` activation_time=${activationTime} wait_ms=${waitMs} order_no=${orderNo}`
  );
}
