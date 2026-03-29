# DOMAIN GUIDE

Apply root `AGENTS.md` first. This file covers feature slices under `domain/`.

## OVERVIEW
Business logic is organized by feature slice, usually with `controller`, `converter`, `domain`, `dto`, `enums`, `exception`, `repository`, and `service` subpackages.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Member/account flows | `member/` | auth-adjacent domain code |
| Party lifecycle | `party/` | joins, invitations, roles, events |
| Exercise lifecycle | `exercise/` | largest slice; read child guide |
| Chat realtime flows | `chat/` | read child guide |
| Notifications | `notification/` | FCM-backed notifications |
| Simpler slices | `bookmark/`, `contest/`, `file/`, `terms/` | mostly standard pattern |

## CONVENTIONS
- Domain-specific errors live in each slice’s `exception/*ErrorCode.java`.
- Controllers expose `/api/...` endpoints and use global response wrappers.
- Shared enums/base entities come from `global/`; slice-specific rules stay here.
- `party/` and `chat/` add `events/`; `party/` also has `utils/`.
- QueryDSL generated code mirrors entity packages elsewhere; do not mix handwritten logic into generated folders.

## ANTI-PATTERNS
- Do not move business rules into `global/` just because multiple slices depend on them.
- Do not bypass slice error codes with ad hoc strings or generic exceptions.
- Do not edit generated Q-types to “fix” repository behavior.

## CHILD GUIDES
- `chat/AGENTS.md`
- `exercise/AGENTS.md`
