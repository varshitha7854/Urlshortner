import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    redirects: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 25),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const shortCode = __ENV.SHORT_CODE;

export default function () {
  const response = http.get(`${baseUrl}/${shortCode}`, { redirects: 0 });
  check(response, {
    'redirects with 302': (r) => r.status === 302,
    'has location header': (r) => Boolean(r.headers.Location),
  });
  sleep(0.1);
}
