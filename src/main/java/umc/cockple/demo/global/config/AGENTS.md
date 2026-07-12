# CONFIG GUIDE

Apply parent guides first. This file only covers `global/config/`.

## OVERVIEW
This package is the shared runtime bootstrap surface for security, Redis/cache, Firebase, Swagger, QueryDSL, async work, and external storage wiring.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Security whitelist + CORS | `SecurityConfig.java` | explicit public endpoints and allowed origins |
| Redis/cache serialization | `RedisConfig.java` | connection factory, templates, cache manager |
| Firebase init | `FirebaseConfig.java` | disabled for `integrationtest` profile |

## CONVENTIONS
- Runtime values belong in `application*.yml`; config classes wire beans around those values.
- `SecurityConfig` carries explicit HTTP frontend origin lists. Chat WebSocket origins live in `domain/chat/presentation/websocket/ChatWebSocketConfig.java`.
- Firebase is suppressed during integration tests and mocked from test config.
- Redis serialization uses a permissive polymorphic JSON serializer; cache/template behavior lives here, not in slices.

## ANTI-PATTERNS
- Do not widen public endpoints or origins without understanding auth and deployment impact.
- Do not embed feature logic in config beans.
- Do not duplicate profile gating from `application*.yml` inside random services.
