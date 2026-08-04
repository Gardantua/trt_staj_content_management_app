import http from 'k6/http';
import { check, sleep } from 'k6';

const profiles = {
  baseline: {
    executor: 'constant-arrival-rate',
    rate: 10,
    timeUnit: '1s',
    duration: '1m',
    preAllocatedVUs: 10,
    maxVUs: 30,
  },
  ramp: {
    executor: 'ramping-arrival-rate',
    startRate: 5,
    timeUnit: '1s',
    preAllocatedVUs: 20,
    maxVUs: 100,
    stages: [
      { target: 25, duration: '1m' },
      { target: 50, duration: '2m' },
      { target: 5, duration: '30s' },
    ],
  },
  spike: {
    executor: 'ramping-arrival-rate',
    startRate: 10,
    timeUnit: '1s',
    preAllocatedVUs: 50,
    maxVUs: 250,
    stages: [
      { target: 10, duration: '20s' },
      { target: 150, duration: '10s' },
      { target: 150, duration: '30s' },
      { target: 10, duration: '20s' },
    ],
  },
  soak: {
    executor: 'constant-arrival-rate',
    rate: 20,
    timeUnit: '1s',
    duration: '10m',
    preAllocatedVUs: 20,
    maxVUs: 60,
  },
};

const selectedProfile = __ENV.LOAD_PROFILE || 'baseline';

if (!profiles[selectedProfile]) {
  throw new Error(`Unknown LOAD_PROFILE '${selectedProfile}'. Use baseline, ramp, spike, or soak.`);
}

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    content_read: profiles[selectedProfile],
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    dropped_iterations: ['count==0'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
const requestParams = {
  headers: {
    'X-Test-Actor-Id': '11111111-1111-1111-1111-111111111111',
    'X-Test-Actor-Roles': 'USER',
  },
  tags: { endpoint: 'published-content-list' },
};

export default function () {
  const response = http.get(
    `${baseUrl}/api/v1/contents?page=0&size=20`,
    requestParams,
  );

  check(response, {
    'content list returns 200': (result) => result.status === 200,
    'response includes trace id': (result) => Boolean(result.headers['X-Trace-Id']),
  });

  sleep(0.05);
}
