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
| Chat session selection | `presentation/websocket/session/ChatWebSocketSessionRegistry.java` | selects the latest open legacy-chat session from the shared multi-session registry |
| WebSocket response assembly | `converter/ChatWebSocketResponseAssembler.java` | shapes socket response payloads |
| Command flows | `service/command/` | direct-room creation and party-room lifecycle |
| Chat file flows | `service/file/ChatFileService.java` | download-token issuance and file download |
| Read/query flows | `service/query/` | party/direct room lists, unread status, room detail, message history, party room IDs |
| Query message assembly | `service/support/assembler/ChatMessageViewAssembler.java` | builds shared message views for REST queries and WebSocket sends |
| Realtime services | `service/websocket/` | subscription, room list cache, message fanout |
| WebSocket request validation | `service/websocket/validation/ChatWebSocketRequestValidator.java` | validates authenticated socket command payloads before handling |
| Service event listeners | `service/listener/` | handles chat, party lifecycle, subscription, and member withdrawal events |
| Member lifecycle cleanup | `service/ChatMemberAnonymizationService.java`, `service/ChatMemberHardDeleteCleanupService.java` | owns withdrawal anonymization and hard-delete cleanup |
| Events | `events/` | send/subscription events bridge transport and async handlers |
| DTO conversion | `converter/ChatConverter.java` | shapes REST/API and common message payloads |

## CONVENTIONS
- `presentation/websocket/ChatWebSocketConfig` registers `/ws/chats` and wires the shared JWT handshake interceptor.
- Request `type()` drives socket branching: send, subscribe, unsubscribe, and chat-list variants.
- Party chat and direct chat share the slice but differ in display-name/image/read-status logic.
- REST query endpoints depend directly on the use-case services under `service/query/`.
- Room list freshness depends on `service/websocket/ChatRoomListCacheService`.

## ANTI-PATTERNS
- Do not treat chat as HTTP-only; transport, cache, and event flow are part of the slice.
- Do not bypass membership/access validation before room or message operations.
- Do not reintroduce a catch-all query facade over the use-case query services.
- Do not mix socket session bookkeeping into controllers or converters.
