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
| HTTP surface | `controller/ExerciseController.java` | CRUD + guests + calendars |
| Error rules | `exception/ExerciseErrorCode.java` | started/past-time/permission constraints |
| Integration coverage | `src/test/java/umc/cockple/demo/domain/exercise/` | biggest test package in repo |

## CONVENTIONS
- Time/date/location validation is centralized and reused through service methods and error codes.
- Participation logic distinguishes confirmed participants from waiting members/guests.
- Guest invitation rules depend on both party membership and exercise flags.
- Converter growth is already high; new mapping code should stay tightly scoped to one flow.

## ANTI-PATTERNS
- Do not bypass `EXERCISE4xx` guardrails for past/start-state checks.
- Do not duplicate participant/waiting-list logic in controllers or tests.
- Do not spread unrelated conversions into `ExerciseConverter` without checking for an existing narrower path first.
