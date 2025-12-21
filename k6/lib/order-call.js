import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import {SharedArray} from 'k6/data';

const [secret] = new SharedArray('secret', () => {
    return [JSON.parse(open('./secret.json'))]
});
const BASE_URL = secret.baseUrl;
export const successCount = new Counter('successful_orders');
export const failCount = new Counter('failed_orders');

export function runTest(data) {
    const memberNo = __VU * 1000000 + __ITER;
    const payload = JSON.stringify({
        productNo: 1,
        count: 1,
    });
    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'OrderReservation' }
    };

    const res = http.post(`${BASE_URL}/api/v1/orders?memberNo=${memberNo}`, payload, params);

    const isSuccess = check(res, {
        'status is 201': (r) => r.status === 201,
        'status is not 500': (r) => r.status !== 500,
    });
    if (isSuccess) {
        successCount.add(1);
    } else {
        failCount.add(1);
        console.log(`❌ Order Failed! Status: ${res.status}, Body: ${res.body}`);
    }

    sleep(1);
}