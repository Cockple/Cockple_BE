# CHAT GUIDE

Apply parent guides first. This file only covers `domain/chat/`.

## OVERVIEW
Chat mixes REST queries with WebSocket transport, Redis-backed subscription/cache behavior, and domain events.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| WebSocket ingress | `presentation/websocket/ChatWebSocketHandler.java` | handles connect/message/close/error |
| Auth for sockets | `global/realtime/auth/` | Shared JWT handshake auth is enforced before handler logic |
| WebSocket dispatch | `presentation/websocket/ChatWebSocketRequestDispatcher.java` | parses socket payloads and delegates authenticated requests |
| WebSocket commands | `presentation/websocket/ChatWebSocketCommandHandler.java` | handles socket request types and publishes command events |
| WebSocket responses | `presentation/websocket/WebSocketResponseSender.java` | shapes chat responses and delegates encoding/session writes to shared realtime infra |
| WebSocket response assembly | `converter/ChatWebSocketResponseAssembler.java` | shapes socket response payloads |
| Realtime services | `service/websocket/` | subscription, room list cache, message fanout |
| Read/query flows | `service/query/ChatQueryServiceImpl.java` | room lists, unread counts, history |
| Events | `events/` | send/subscription events bridge transport and async handlers |
| DTO conversion | `converter/ChatConverter.java` | shapes REST/API and common message payloads |

## CONVENTIONS
- `presentation/websocket/ChatWebSocketConfig` registers `/ws/chats` and wires the shared JWT handshake interceptor.
- Request `type()` drives socket branching: send, subscribe, unsubscribe, and chat-list variants.
- Party chat and direct chat share the slice but differ in display-name/image/read-status logic.
- Room list freshness depends on `service/websocket/ChatRoomListCacheService`.

## ANTI-PATTERNS
- Do not treat chat as HTTP-only; transport, cache, and event flow are part of the slice.
- Do not bypass membership/access validation before room or message operations.
- Do not mix socket session bookkeeping into controllers or converters.
