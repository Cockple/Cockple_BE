# GLOBAL GUIDE

Apply root `AGENTS.md` first. This file covers `global/` cross-cutting code.

## OVERVIEW
`global/` holds shared framework glue: response/exception handling, infra config, security, JWT, OAuth, realtime transport, shared enums, and the common base entity.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Bootstrap/config wiring | `config/` | see child guide |
| Response wrapper | `response/` | `BaseResponse`, code DTOs, success/error codes |
| Exception handling | `exception/` | global handler + shared exception base |
| Security/JWT | `security/`, `jwt/` | auth filter + token creation/parsing |
| Realtime transport | `realtime/` | WebSocket handshake auth, MDC, message encoding, session-level writes, and multi-session registry |
| OAuth | `oauth2/` | Kakao-specific login flow |
| Shared types | `common/BaseEntity.java`, `enums/` | audit fields and shared enums |

## CONVENTIONS
- API success/error formatting is centralized here; slices should plug into it rather than invent local wrappers.
- `jwt/` and `oauth2/` are cross-cutting in placement but still coupled to member/auth flows.
- `common/BaseEntity` and shared enums are the stable lowest-level shared types.

## ANTI-PATTERNS
- Do not put feature-specific business rules here unless they are truly shared.
- Do not assume JWT/OAuth classes are provider-agnostic; this repo is Kakao-specific.
- Do not bypass shared response/error abstractions from controllers.

## CHILD GUIDES
- `config/AGENTS.md`
