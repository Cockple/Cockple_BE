# TEST GUIDE

Apply root `AGENTS.md` first. This file covers `src/test/java/umc/cockple/demo/`.

## OVERVIEW
Tests mirror production domains and split into integration tests with Testcontainers/MockMvc and service tests with Mockito. Shared support code lives under `support/`.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Integration base | `support/IntegrationTestBase.java` | `@SpringBootTest`, `MockMvc`, `integrationtest` profile |
| Testcontainers + mocks | `support/IntegrationTestConfig.java` | MySQL, Redis, mocked FirebaseMessaging |
| Auth helpers | `support/SecurityContextHelper.java` | sets and clears security context |
| Shared fixtures | `support/fixture/` | static factories for member/party/exercise/chat/etc. |
| Feature integration tests | `domain/*/integration/` | HTTP-level assertions and manual cleanup |
| Feature service tests | `domain/*/service/` | Mockito-based isolated logic tests |

## CONVENTIONS
- Integration tests extend `IntegrationTestBase` and usually clear repositories in `@AfterEach`.
- Service tests use `@ExtendWith(MockitoExtension.class)` and shared fixtures.
- `application.yml` in test resources uses H2; `application-integrationtest.yml` switches to MySQL/Redis Testcontainers.
- Large exercise/member/chat tests follow the same nested-class style; keep new tests aligned with that structure.

## ANTI-PATTERNS
- Do not create per-feature fixture factories when an existing shared fixture can be extended.
- Do not mix Mockito-style unit tests and full integration setup in the same class.
- Do not forget `SecurityContextHelper.clearAuthentication()` in integration teardown paths.
