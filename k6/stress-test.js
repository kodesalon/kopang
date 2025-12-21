import {runTest} from './lib/order-call.js';

export const options = {
    scenarios: {
        open_run_event: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                {target: 10, duration: '10s'},
                {target: 300, duration: '10s'},
                {target: 300, duration: '40s'},
                {target: 0, duration: '10s'},
            ],
        },
    },

    thresholds: {
        'http_req_failed{status:500}': ['rate==0'], // 500 에러는 절대 없어야됨
        http_req_duration: ['p(95)<3000'],
    },
};

export default function (data) {
    runTest(data);
}