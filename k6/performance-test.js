// cache-perf-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

// 1. 설정 로드
const [secret] = new SharedArray('secret', () => {
    return [JSON.parse(open('./lib/secret.json'))]
});
const BASE_URL = secret.baseUrl;

// 2. 커스텀 지표
export const successCount = new Counter('successful_orders');
export const failCount = new Counter('failed_orders');

// 3. 테스트 시나리오 (Spike Test)
export const options = {
    scenarios: {
        // 시나리오 A: 워밍업 (JIT 컴파일러 예열)
        warm_up: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
            startTime: '0s',
            gracefulStop: '5s',
        },
        // 시나리오 B: 선착순 오픈 (Spike) - 10:00:00 상황
        // t3.small 1대가 감당할 극한의 상황을 가정
        fcfs_spike: {
            executor: 'ramping-vus',
            startTime: '35s', // 워밍업 후 실행
            startVUs: 0,
            stages: [
                // [오픈 직전] 5초 만에 300 VU 투입 (수직 상승)
                { target: 300, duration: '5s' },
                // [오픈 중] 2분간 폭주 상태 유지 (병목 지점 확인)
                { target: 300, duration: '2m' },
                // [종료] 빠르게 빠짐
                { target: 0, duration: '10s' },
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        'http_req_failed': ['rate<0.01'], // 에러율 1% 미만 (Business Exception 제외)
        'http_req_duration': ['p(95)<1000'], // Redis Cluster 감안하여 1초 이내
    },
};

export default function () {
    // 4. 유니크 유저 생성 (전과 동일)
    const uniqueId = exec.scenario.iterationInTest;
    const memberNo = (uniqueId % 20000) + 1; // 유저 풀을 좀 더 늘림 (동시성 이슈 강화)

    const payload = JSON.stringify({
        productNo: 1,
        count: 1,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'Hot_Order' } // 태그로 필터링 가능
    };

    // 5. 요청 수행
    const res = http.post(`${BASE_URL}/api/v1/orders?memberNo=${memberNo}`, payload, params);

    // 6. 결과 처리
    // 선착순은 '재고 없음(4xx)'도 시스템 관점에서는 '성공적인 처리'임. 5xx만 실패로 간주.
    const isSystemSuccess = check(res, {
        'system handled request': (r) => r.status === 200 || r.status === 201 || r.status === 400 || r.status === 409,
    });

    if (isSystemSuccess) {
        // 비즈니스 성공(주문 완료)만 별도 카운팅
        if (res.status === 200 || res.status === 201) {
            successCount.add(1);
        }
    } else {
        failCount.add(1);
        // 에러 샘플링 로그
        if (exec.scenario.iterationInTest % 100 === 0) {
            console.log(`❌ System Fail: ${res.status} ${res.body}`);
        }
    }

    // 7. 선착순 유저 행동 패턴 (광클)
    // 일반적인 상황보다 훨씬 짧게 대기하거나, 실패 시 대기 없이 재시도하는 로직 구현 가능
    // 여기서는 매우 짧은 sleep으로 트래픽 압박 유지
    sleep(0.005); // 5ms 대기 (거의 쉬지 않음)
}