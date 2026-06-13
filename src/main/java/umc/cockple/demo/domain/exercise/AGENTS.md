# EXERCISE GUIDE

Apply parent guides first. This file only covers `domain/exercise/`.

## OVERVIEW
Exercise is the densest feature slice: scheduling, guests, participation, waiting lists, recommendations, map/calendar queries, and the largest integration/service tests.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Read-heavy hotspot | `service/ExerciseQueryService.java` | calendar/detail/recommendation/building/map queries |
| Command internals | `service/command/internal/` | guest, lifecycle, participation subflows |
| DTO mapping hotspot | `converter/ExerciseConverter.java` | very large conversion surface |
| HTTP surface | `controller/` | split by exercise use case; see ownership map below |
| Error rules | `exception/ExerciseErrorCode.java` | started/past-time/permission constraints |
| Integration coverage | `src/test/java/umc/cockple/demo/domain/exercise/` | biggest test package in repo |

## CONTROLLER OWNERSHIP
| Controller | Owns |
|------------|------|
| `ExerciseLifecycleController` | exercise create/delete/update/detail/for-edit root resource endpoints |
| `ExerciseParticipationController` | participant join/cancel flows under `/api/exercises/{exerciseId}/participants` |
| `ExerciseGuestController` | guest invite/list/cancel flows under `/api/exercises/{exerciseId}/guests` |
| `ExerciseMyController` | member-centric exercise views: my exercises, my exercise calendar, my party exercises |
| `PartyExerciseController` | party-scoped exercise calendar under `/api/parties/{partyId}/exercises` |
| `ExerciseRecommendationController` | recommended exercises and recommended exercise calendar |
| `ExerciseMapController` | building exercise details and monthly building map endpoints |

## CONVENTIONS
- Time/date/location validation is centralized and reused through service methods and error codes.
- Participation logic distinguishes confirmed participants from waiting members/guests.
- Guest invitation rules depend on both party membership and exercise flags.
- Converter growth is already high; new mapping code should stay tightly scoped to one flow.
- Keep all exercise HTTP controllers grouped in Swagger with `@Tag(name = "Exercise", description = "운동 관리 API")`.
- Controllers should return `ResponseEntity<BaseResponse<...>>` via `BaseResponse.of(...)`.
- Preserve existing public paths when lifting common `@RequestMapping` prefixes; class-level mapping changes should be route-compatible.
- Integration tests should mirror controller/use-case ownership, e.g. `ExerciseGuestIntegrationTest`, `ExerciseMyIntegrationTest`, `ExerciseMapIntegrationTest`.

## ANTI-PATTERNS
- Do not bypass `EXERCISE4xx` guardrails for past/start-state checks.
- Do not duplicate participant/waiting-list logic in controllers or tests.
- Do not spread unrelated conversions into `ExerciseConverter` without checking for an existing narrower path first.
- Do not recreate a monolithic `ExerciseController`; add or adjust the focused controller that owns the endpoint family.
