# CHAT GUIDE

Apply parent guides first. This file only covers `domain/chat/`.

## OVERVIEW
Chat mixes REST queries with WebSocket transport, Redis-backed subscription/cache behavior, and domain events.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| WebSocket ingress | `handler/ChatWebSocketHandler.java` | handles connect/message/close/error |
| Auth for sockets | `interceptor/` | JWT auth is enforced before handler logic |
| Realtime services | `service/websocket/` | subscription, room list cache, message fanout |
| Read/query flows | `service/ChatQueryServiceImpl.java` | room lists, unread counts, history |
| Events | `events/` | send/subscription events bridge transport and async handlers |
| DTO conversion | `converter/ChatConverter.java` | shapes REST/socket payloads |

## CONVENTIONS
- `WebSocketConfig` registers `/ws/chats` and wires the JWT interceptor.
- Request `type()` drives socket branching: send, subscribe, unsubscribe, and chat-list variants.
- Party chat and direct chat share the slice but differ in display-name/image/read-status logic.
- Room list freshness depends on `service/websocket/ChatRoomListCacheService`.

## ANTI-PATTERNS
- Do not treat chat as HTTP-only; transport, cache, and event flow are part of the slice.
- Do not bypass membership/access validation before room or message operations.
- Do not mix socket session bookkeeping into controllers or converters.
