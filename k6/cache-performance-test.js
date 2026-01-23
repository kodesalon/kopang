// cache-perf-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// 1. 설정 로드
const config = new SharedArray('config', () => {
    return JSON.parse(open('./secret.json'));
});
const BASE_URL = config[0].baseUrl;

// 2. 커스텀 지표
export const successCount = new Counter('successful_orders');
export const failCount = new Counter('failed_orders');

// 3. 테스트 옵션 (시나리오)
export const options = {
    scenarios: {
        hot_key_stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { target: 50, duration: '30s' },  // [Warm-up] JVM 워밍업 및 캐시 적재
                { target: 200, duration: '1m' },  // [Load] 본 게임: t3.small 기준 200명 동시 요청은 매우 무거움
                { target: 200, duration: '2m' },  // [Sustain] 병목 지속 관찰
                { target: 0, duration: '30s' },   // [Cool-down]
            ],
        },
    },

    // 임계치 설정 (SLA)
    thresholds: {
        'http_req_failed': ['rate<0.01'], // 에러율 1% 미만
        'http_req_duration': ['p(95)<500'], // 95% 요청이 0.5초 안에 끝나야 함 (Redis 지연 시 깨질 예정)
    },
};

export default function () {
    // 4. Hot Key & Fixed User 전략
    // 앞서 DB에 넣어둔 1번 유저(강남 거주), 1번 상품(인기 상품) 고정
    const memberNo = 1;

    const payload = JSON.stringify({
        productNo: 1,
        count: 1,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'HotKey_Order' } // 태그로 필터링 가능
    };

    // 5. 요청 수행
    const res = http.post(`${BASE_URL}/api/v1/orders?memberNo=${memberNo}`, payload, params);

    // 6. 검증
    const isSuccess = check(res, {
        'status is 200/201': (r) => r.status === 200 || r.status === 201,
    });

    if (isSuccess) {
        successCount.add(1);
    } else {
        failCount.add(1);
        // 에러 로그는 너무 많이 찍히면 성능 저하되므로 간헐적으로만 확인하거나 주석 처리
        if (failCount.name % 100 === 0) {
            console.log(`❌ Fail: ${res.status} ${res.body}`);
        }
    }

    // 7. 짧은 대기 (Throttling)
    // 0.01초 대기 -> VU당 초당 약 50~80회 요청 시도 (시스템 한계까지 밀어붙임)
    sleep(0.01);
}