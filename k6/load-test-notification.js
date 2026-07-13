import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ========== 설정 ==========
const BASE_URL = 'https://staging.cockple.store';
const ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwibmlja25hbWUiOiLquLjrj5nsnbQiLCJpYXQiOjE3Nzk0NjM5ODksImV4cCI6MTc3OTQ2NDg4OX0.PbiAgiQj8x7oEFdLwPO7BSvENAeY1GEPJVDDcljz6u8'; // 로그인 API로 발급받은 토큰 입력
const PARTY_ID = 6;
// ==========================

const errorRate = new Rate('errors');
const notificationDuration = new Trend('notification_duration', true);

export const options = {
  scenarios: {
    // 1단계: 정상 동작 확인 (1 VU, 30초)
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      tags: { scenario: 'smoke' },
    },
    // 2단계: 5배 부하 (10 VU)
    load_5x: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 }, // ramp up
        { duration: '3m', target: 10 }, // 유지
        { duration: '1m', target: 0 },  // ramp down
      ],
      startTime: '1m',
      tags: { scenario: 'load_5x' },
    },
    // 3단계: 25배 부하 (50 VU)
    load_25x: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 }, // ramp up
        { duration: '3m', target: 50 }, // 유지
        { duration: '1m', target: 0 },  // ramp down
      ],
      startTime: '7m',
      tags: { scenario: 'load_25x' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    errors: ['rate<0.05'],
  },
};

export default function () {
  const res = http.post(
    `${BASE_URL}/api/test/notification?partyId=${PARTY_ID}`,
    null,
    {
      headers: {
        Authorization: `Bearer ${ACCESS_TOKEN}`,
        'Content-Type': 'application/json',
      },
    }
  );

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  if (!success) {
    console.log(`에러 - status: ${res.status}, body: ${res.body}`);
  }

  errorRate.add(!success);
  notificationDuration.add(res.timings.duration);

  sleep(1);
}
