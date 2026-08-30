# CHAT GUIDE

Apply parent guides first. This file only covers `domain/chat/`.

## OVERVIEW
Chat mixes REST queries with WebSocket transport, Redis-backed subscription/cache behavior, and domain events.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| REST API contracts | `presentation/rest/api/` | owns request mappings, parameter bindings, and OpenAPI metadata for chat REST APIs |
| Party chat REST | `presentation/rest/PartyChatController.java` | handles party room lists, search, and party room ID lookup |
| Direct chat REST | `presentation/rest/DirectChatController.java` | handles direct-room creation, lists, and search |
| Chat room REST | `presentation/rest/ChatRoomController.java` | handles unread status, room detail, and message history |
| Chat file REST | `presentation/rest/ChatFileController.java` | handles download-token issuance and file responses |
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
- Interfaces under `presentation/rest/api/` own REST mappings, parameter annotations, and Swagger documentation.
- Concrete REST controllers only resolve caller context when required, delegate to their use-case services, and shape the response.
- `presentation/rest/api/ChatApiTag.java` keeps the split REST APIs under the shared `Chat` Swagger tag.
- `presentation/websocket/ChatWebSocketConfig` registers `/ws/chats` and wires the shared JWT handshake interceptor.
- Request `type()` drives socket branching: send, subscribe, unsubscribe, and chat-list variants.
- Party chat and direct chat share the slice but differ in display-name/image/read-status logic.
- REST query endpoints depend directly on the use-case services under `service/query/`.
- Room list freshness depends on `service/websocket/ChatRoomListCacheService`.

## ANTI-PATTERNS
- Do not treat chat as HTTP-only; transport, cache, and event flow are part of the slice.
- Do not bypass membership/access validation before room or message operations.
- Do not collapse party, direct, room, and file REST responsibilities back into one catch-all controller.
- Do not duplicate REST mappings or Swagger annotations in concrete controllers; keep the API interfaces authoritative.
- Do not reintroduce a catch-all query facade over the use-case query services.
- Do not mix socket session bookkeeping into controllers or converters.
