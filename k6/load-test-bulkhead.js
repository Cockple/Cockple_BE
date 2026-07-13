import http from 'k6/http';
import ws from 'k6/ws';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/*
 * ============================================================================
 *  스레드풀 분리(벌크헤드) Before/After 비교용 부하 테스트
 * ============================================================================
 *
 *  [목적]
 *   FCM(외부 I/O)이 느려질 때 "채팅 실시간 지연"이 같이 폭증하는지 측정한다.
 *   - Before(단일 풀): FCM이 풀을 점유 -> 채팅 지연 동반 폭증 (예상)
 *   - After(풀 분리) : FCM이 느려도 채팅 지연은 안정 (기대)
 *
 *  ★ 측정 핵심 지표 = chat_e2e_latency (채팅 전송->수신 왕복 지연)
 *    알림 지연이 아니라 "채팅" 지연을 봐야 분리 효과가 드러난다.
 *
 *  [전제 - 서버 쪽]
 *   1) FcmService 전송 경로에 인위적 지연(Thread.sleep)을 주입한 빌드를 배포할 것
 *   2) staging 프로파일(= /api/test/notification 활성)
 *   3) Before 측정 후 풀 분리 리팩토링 -> 동일 조건으로 After 측정
 *
 *  [전제 - 토큰/방]
 *   - TOKEN_SENDER, TOKEN_RECEIVER 는 서로 다른 멤버이고, 둘 다 CHAT_ROOM_ID 방의 참여자여야 한다.
 *     (발신자는 자기 메시지를 브로드캐스트로 못 받으므로 수신자를 별도 멤버로 둔다)
 *   - PARTY_ID 는 알림 유발용 파티 ID (TOKEN_SENDER 가 접근 가능한 파티)
 *
 *  [실행 예]
 *   k6 run \
 *     -e BASE_URL=https://staging.cockple.store \
 *     -e TOKEN_SENDER=eyJ... \
 *     -e TOKEN_RECEIVER=eyJ... \
 *     -e CHAT_ROOM_ID=1 \
 *     -e PARTY_ID=6 \
 *     --summary-export=result-bulkhead-before.json \
 *     k6/load-test-bulkhead.js
 *
 *   Before/After 는 --summary-export 파일명만 바꿔 두 번 돌린 뒤 비교한다.
 * ============================================================================
 */

// ========== 설정 (env 로 override) ==========
const BASE_URL = __ENV.BASE_URL || 'https://staging.cockple.store';
const TOKEN_SENDER = __ENV.TOKEN_SENDER || '';
const TOKEN_RECEIVER = __ENV.TOKEN_RECEIVER || '';
const CHAT_ROOM_ID = Number(__ENV.CHAT_ROOM_ID || 1);
const PARTY_ID = Number(__ENV.PARTY_ID || 6);

// 채팅 전송 주기(ms). 작을수록 채팅 풀에 더 많은 작업을 태운다.
const SEND_INTERVAL_MS = Number(__ENV.SEND_INTERVAL_MS || 200); // 5 msg/s
// WS 세션 1회 지속 시간(s). 이 시간이 지나면 끊고 재접속(재시도).
const SESSION_SECONDS = Number(__ENV.SESSION_SECONDS || 330);
// 알림 스트레스 최대 VU (FCM 풀 포화용)
const NOTIF_MAX_VUS = Number(__ENV.NOTIF_MAX_VUS || 50);

// ws(s) URL 파생
const WS_URL =
  BASE_URL.replace(/^http/, 'ws') + '/ws/chats/websocket?token=';

// ========== 커스텀 메트릭 ==========
const chatLatency = new Trend('chat_e2e_latency', true); // ★ 핵심 지표 (ms)
const chatReceived = new Counter('chat_messages_received');
const chatSent = new Counter('chat_messages_sent');
const wsErrors = new Rate('ws_errors');
const notifDuration = new Trend('notif_req_duration', true);
const notifErrors = new Rate('notif_errors');

// ========== 시나리오 ==========
// 타임라인(약 5분30초):
//   0~1m   : 채팅만 흐름 -> 채팅 baseline 지연 확보
//   1~4m   : 알림(FCM) 스트레스 ON -> 채팅 지연 전이 여부 관찰  ★스토리 구간
//   4~5m   : 스트레스 OFF -> 회복 관찰
export const options = {
  scenarios: {
    // 채팅 수신자(측정 주체) - 테스트 내내 유지
    chat_receiver: {
      executor: 'constant-vus',
      vus: 1,
      duration: '5m30s',
      exec: 'chatReceiver',
      tags: { role: 'chat_receiver' },
    },
    // 채팅 발신자 - 테스트 내내 유지
    chat_sender: {
      executor: 'constant-vus',
      vus: 1,
      duration: '5m30s',
      exec: 'chatSender',
      tags: { role: 'chat_sender' },
    },
    // 알림 스트레스 - 1분 뒤 시작(채팅 baseline 확보 후)
    notification_stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '1m',
      exec: 'notificationLoad',
      stages: [
        { duration: '1m', target: NOTIF_MAX_VUS }, // ramp up
        { duration: '2m', target: NOTIF_MAX_VUS }, // 유지(스트레스 구간)
        { duration: '1m', target: 0 },             // ramp down
      ],
      tags: { role: 'notification_stress' },
    },
  },
  thresholds: {
    // 합/불 용도가 아니라 참고용. 비교는 export JSON 의 percentile 로 한다.
    'chat_e2e_latency': ['p(95)<3000'],
    'notif_errors': ['rate<0.10'],
  },
};

// ========== SockJS 프레이밍 헬퍼 ==========
// .withSockJS() 환경에서 /websocket 은 보통 SockJS 프레임('o','a[...]','h','c')을 쓴다.
// 혹시 raw 패스스루('{...}')면 자동 감지해서 모드를 전환한다.
function parseFrames(raw, state) {
  if (typeof raw !== 'string' || raw.length === 0) return [];
  if (raw === 'o') { state.sockjs = true; return ['__OPEN__']; }
  if (raw === 'h') return []; // heartbeat 무시
  const c = raw[0];
  if (c === 'a') { // 메시지 배열
    try { return JSON.parse(raw.slice(1)); } catch (e) { return []; }
  }
  if (c === 'm') { // 단일 메시지
    try { return [JSON.parse(raw.slice(1))]; } catch (e) { return []; }
  }
  if (c === 'c') { state.closed = true; return []; } // close
  if (c === '{' || c === '[') { // raw 패스스루(프레이밍 없음)
    state.sockjs = false;
    return [raw];
  }
  return [];
}

function wsSend(socket, state, payloadObj) {
  const json = JSON.stringify(payloadObj);
  if (state.sockjs) {
    socket.send(JSON.stringify([json])); // SockJS: 문자열 배열 프레임
  } else {
    socket.send(json); // raw: 그대로
  }
}

// 공통 WS 연결 로직. role 에 따라 동작 분기.
function connectWs(token, role) {
  if (!token) {
    console.error(`[${role}] 토큰이 비어있음 - TOKEN_SENDER/TOKEN_RECEIVER 확인`);
    wsErrors.add(1);
    return;
  }

  const state = { sockjs: true, subscribed: false, closed: false };

  const res = ws.connect(WS_URL + token, {}, (socket) => {
    function ensureSubscribed() {
      if (state.subscribed) return;
      state.subscribed = true;
      wsSend(socket, state, { type: 'SUBSCRIBE', chatRoomId: CHAT_ROOM_ID });

      if (role === 'sender') {
        // 구독 후 주기적으로 메시지 전송 (content 에 전송 시각 epoch 를 심는다)
        socket.setInterval(() => {
          const sentAt = Date.now();
          wsSend(socket, state, {
            type: 'SEND',
            chatRoomId: CHAT_ROOM_ID,
            content: `LT|${sentAt}|${__VU}`,
            images: [],
          });
          chatSent.add(1);
        }, SEND_INTERVAL_MS);
      }
    }

    socket.on('open', () => {
      // raw 모드일 수도 있으니, 일정 시간 내 프레임이 없으면 가정 하에 구독 시도
      socket.setTimeout(() => ensureSubscribed(), 1500);
    });

    socket.on('message', (raw) => {
      const msgs = parseFrames(raw, state);
      for (const m of msgs) {
        if (m === '__OPEN__') { ensureSubscribed(); continue; }

        // 첫 실제 프레임 수신 시점에도 구독 보장(raw 모드 대비)
        ensureSubscribed();

        if (role !== 'receiver') continue;

        let obj;
        try { obj = JSON.parse(m); } catch (e) { continue; }

        // 우리가 심은 측정용 메시지만 집계 (content: "LT|<epoch>|<vu>")
        if (obj && typeof obj.content === 'string' && obj.content.indexOf('LT|') === 0) {
          const parts = obj.content.split('|');
          const sentAt = Number(parts[1]);
          if (sentAt > 0) {
            chatLatency.add(Date.now() - sentAt);
            chatReceived.add(1);
          }
        }
      }
    });

    socket.on('error', (e) => {
      wsErrors.add(1);
      console.error(`[${role}] ws error: ${e && e.error ? e.error() : e}`);
    });

    // 세션 수명 종료 -> 닫고 iteration 종료(이후 재접속)
    socket.setTimeout(() => socket.close(), SESSION_SECONDS * 1000);
  });

  check(res, { '[ws] status 101': (r) => r && r.status === 101 });
  if (!res || res.status !== 101) wsErrors.add(1);
}

// ========== exec 함수들 ==========
export function chatReceiver() {
  connectWs(TOKEN_RECEIVER, 'receiver');
}

export function chatSender() {
  connectWs(TOKEN_SENDER, 'sender');
}

export function notificationLoad() {
  const res = http.post(
    `${BASE_URL}/api/test/notification?partyId=${PARTY_ID}`,
    null,
    {
      headers: {
        Authorization: `Bearer ${TOKEN_SENDER}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'notification' },
    }
  );

  const ok = check(res, { '[notif] status 200': (r) => r.status === 200 });
  notifErrors.add(!ok);
  notifDuration.add(res.timings.duration);
  if (!ok) {
    console.log(`[notif] status=${res.status} body=${res.body}`);
  }
}
