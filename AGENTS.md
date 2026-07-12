# PROJECT KNOWLEDGE BASE

**Generated:** 2026-03-23 20:04 KST
**Commit:** 5af1ffe6
**Branch:** develop

## OVERVIEW
Cockple_BE is a single-module Spring Boot 3.5.9 backend for the Cockple badminton platform. Core stack: Java 17, JPA + QueryDSL, MySQL, Redis, JWT/Kakao OAuth, WebSocket chat, Firebase FCM, Flyway, Docker Compose, and Terraform-managed GCP/Cloudflare infra.

Nearest `AGENTS.md` wins. Read this file first, then the closest child file for the area you are changing.

## STRUCTURE
```text
./
├── .github/workflows/                         # CI/CD entrypoint
├── scripts/                                   # deploy and SSH tunnel helpers
├── terraform/                                 # GCP + Cloudflare infra
├── nginx/                                     # reverse proxy for prod/staging apps
├── src/main/java/umc/cockple/demo/domain/     # business slices
├── src/main/java/umc/cockple/demo/global/     # cross-cutting infra
├── src/main/resources/                        # Spring profiles + Flyway
├── src/main/generated/                        # generated QueryDSL Q-types
└── src/test/java/umc/cockple/demo/            # integration/service tests + fixtures
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App bootstrap | `src/main/java/umc/cockple/demo/Application.java` | Enables JPA auditing and caching |
| Runtime config | `src/main/resources/application*.yml` | `local` is default; `staging` and `prod` override DB/Redis |
| Security ingress | `src/main/java/umc/cockple/demo/global/config/` | HTTP security, JWT filters, CORS, shared runtime config |
| Chat WebSocket ingress | `src/main/java/umc/cockple/demo/domain/chat/presentation/websocket/` | `/ws/chats` handler, auth interceptor, endpoint config |
| Business APIs | `src/main/java/umc/cockple/demo/domain/` | Vertical slices by feature |
| Exercise hotspot | `src/main/java/umc/cockple/demo/domain/exercise/` | Largest converter/query/test surface |
| Chat hotspot | `src/main/java/umc/cockple/demo/domain/chat/` | REST + WebSocket + cache/event flow |
| Shared infra | `src/main/java/umc/cockple/demo/global/` | response, exception, jwt, oauth2, config |
| Test conventions | `src/test/java/umc/cockple/demo/` | shared fixtures + integration base live here |
| Local stack | `docker-compose.yml`, `nginx/`, `scripts/` | prod/staging services share one compose file |
| CI/CD | `.github/workflows/` | PR builds on `develop`/`main`; push deploys by branch |
| Infra changes | `terraform/` | GCP compute/network/storage + Cloudflare |

## CONVENTIONS
- Domain code is organized as feature slices, not horizontal layers across the repo.
- Controllers usually return `BaseResponse` or `ResponseEntity<BaseResponse<...>>`.
- Error codes are per-domain enums and use a documented `1xx/2xx/3xx/4xx` meaning split.
- `application.yml` enforces `ddl-auto: validate`; schema changes belong in Flyway under `src/main/resources/db/migration/`.
- QueryDSL generated sources are wired through Gradle; handwritten code lives under `src/main/java`, generated code under `src/main/generated` or `build/generated/...`.
- Tests split into integration (`MockMvc` + Testcontainers profile) and service/unit (`MockitoExtension`) styles.

## ANTI-PATTERNS (THIS PROJECT)
- Do not edit generated QueryDSL Q-types.
- Do not widen security whitelist or CORS origins casually; HTTP origins are explicit in `SecurityConfig` and chat WebSocket origins are explicit in `ChatWebSocketConfig`.
- Do not rely on JPA auto-DDL for schema work; startup uses validation only.
- Do not scatter test fixtures inside feature test packages; shared fixtures already live under `src/test/java/umc/cockple/demo/support/fixture/`.
- Do not assume everything under `global/` is generic; JWT/OAuth code is coupled to member/auth flows.

## UNIQUE STYLES
- `domain/chat` has extra realtime sublayers: `presentation/websocket/`, `events/`, `service/websocket/`.
- `domain/exercise` is the densest slice: large converter, query service, command internals, and the biggest integration tests.
- `domain/party` and `domain/notification` use events/notification wiring more than simpler slices like `bookmark` or `terms`.

## COMMANDS
```bash
./gradlew.bat build
./gradlew.bat test
./gradlew.bat bootRun
docker compose config --services
bash scripts/tunnel.sh <GCP_IP>
```

## CHILD GUIDES
- `.github/workflows/AGENTS.md`
- `scripts/AGENTS.md`
- `terraform/AGENTS.md`
- `src/main/java/umc/cockple/demo/domain/AGENTS.md`
- `src/main/java/umc/cockple/demo/domain/chat/AGENTS.md`
- `src/main/java/umc/cockple/demo/domain/exercise/AGENTS.md`
- `src/main/java/umc/cockple/demo/global/AGENTS.md`
- `src/main/java/umc/cockple/demo/global/config/AGENTS.md`
- `src/test/java/umc/cockple/demo/AGENTS.md`

## NOTES
- CI writes `src/main/resources/application.yml` from GitHub secrets before building.
- CD copies only `docker-compose.yml`, `init-db.sql`, `nginx/`, and `scripts/` to the server; deploy behavior lives in both workflow and shell script.
- Local profile expects MySQL on `3307` and Redis on `6380`; `scripts/tunnel.sh` exposes those ports through SSH forwarding.
