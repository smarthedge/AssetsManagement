# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Full-stack Assets Management application:
- **Frontend**: Angular 21 standalone components, PrimeNG 21, Tailwind CSS v4, Vitest
- **Backend**: Spring Boot 4.0.6, JDK 25, OAuth2 Resource Server + JWT, PostgreSQL, JPA
- **Database**: PostgreSQL with BIGINT keys, audit fields, soft delete, optimistic locking

## Commands

### Backend (`backend/`)
```bash
./mvnw spring-boot:run                                  # Start on port 8080 (requires PostgreSQL + env vars)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # Dev mode with H2 in-memory DB
./mvnw test                                             # Run all tests
./mvnw test -Dtest=AuthServiceTest                      # Run single test class
```

Swagger UI: http://localhost:8080/swagger-ui.html

Required environment variables (production only):
```
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password   # Gmail App Password, not the account password
```

### Frontend (`frontend/`)
```bash
npm start                  # Start on port 4200 (dev server with proxy to :8080)
npm test                   # Run unit tests (Vitest)
npx ng build               # Production build to dist/
npx ng test --watch=false  # Run tests once
```

## Backend Architecture

### Package structure (`com.assetsmanagement`)
- `entity/` — BaseEntity, User, Role, Asset
- `dto/request/` and `dto/response/` — Java records with Jakarta Validation; all DTOs are records, never classes
- `repository/` — Spring Data JPA interfaces; soft-delete list queries use `findByStatusTrue`
- `service/` — `AuthService` owns registration/social-login/password-reset; `JwtTokenService` owns token minting; others own CRUD
- `controller/` — `AuthController` is fully public; all other controllers require `ROLE_ADMIN` via `@PreAuthorize`
- `config/` — `JwtConfig` (RSA-2048 in-memory), `SecurityConfig`, `SecurityBeans` (UserDetailsService), `WebClientConfig` (RestClient bean)
- `exception/` — `GlobalExceptionHandler` maps to HTTP codes; only two exception types in use (see below)

### Auth — two distinct login paths

**Password login** (`POST /api/auth/login`):
`JwtTokenService.authenticate()` → `AuthenticationManager` → `UserDetailsService` in `SecurityBeans` → BCrypt compare → `generateToken()`.

**Social login / PKCE** (`POST /api/auth/social-login`):
Frontend performs PKCE with the provider, obtains an `id_token` (Google/Microsoft) or `access_token` (GitHub/Facebook), and POSTs `{provider, token}` to the backend. `AuthService.socialLogin()` calls `SocialTokenVerifier.verify()` (HTTP call to the provider's API with hardcoded URLs — never client-supplied), then find-or-create the User, then calls `JwtTokenService.generateTokenForUser(user)`. This path bypasses `AuthenticationManager` entirely.

**Password reset**:
`POST /api/auth/forgot-password` — generates a UUID token, stores it with a 1-hour expiry in the `users` table, sends email via Gmail SMTP. Always returns the same response body regardless of whether the email exists (prevents user enumeration).
`POST /api/auth/reset-password` — validates token not expired, updates `password_hash`, clears both reset fields. Token is single-use by construction.

### Key invariants

- **RSA keys are ephemeral**: `JwtConfig` generates a fresh RSA-2048 pair on every startup. All issued tokens are invalidated on restart. There is no persistent key store.
- **Social users have null `passwordHash`**: `SecurityBeans` sets `credentialsNonExpired = (passwordHash != null)`. Social users who attempt password login receive `CredentialsExpiredException` → 401.
- **`/api/auth/**` is fully public**: Declared in `SecurityConfig.permitAll()`. No `@PreAuthorize` is needed on any `AuthController` method.
- **Audit fields are null for anonymous operations**: `BaseEntity.@PrePersist` skips populating `createdByUsername` when the `SecurityContext` principal is `"anonymousUser"`. Self-registration always produces null audit fields — this is intentional.
- **`ddl-auto=validate` in production**: Hibernate validates the schema but never modifies it. Apply `db/migration/V*.sql` scripts manually to PostgreSQL before deploying schema changes.

### User entity — notable fields added in V2

```
password_hash          VARCHAR(255) NULL   -- null for social-only accounts
provider               VARCHAR(50)  NULL   -- "google" | "github" | "facebook" | "microsoft"
provider_account_id    VARCHAR(255) NULL
password_reset_token   VARCHAR(255) NULL
password_reset_expires TIMESTAMP    NULL
```

Composite unique constraint on `(provider, provider_account_id)` is enforced at both DB level (`V2__auth_features.sql`) and JPA level (`@UniqueConstraint` on `@Table`).

### SocialTokenVerifier — provider verification URLs

All URLs are hardcoded constants. Token values are passed as URI template variables (not string-concatenated) to guarantee URL-encoding.

| Provider  | Verification call |
|---|---|
| Google | `GET oauth2.googleapis.com/tokeninfo?id_token={token}` — requires `email_verified=true` |
| GitHub | `GET api.github.com/user` + fallback `GET api.github.com/user/emails` when email is private |
| Facebook | `GET graph.facebook.com/me?fields=id,email,name&access_token={token}` — throws if no email |
| Microsoft | `GET graph.microsoft.com/v1.0/me` — uses `mail`, falls back to `userPrincipalName` |

### Exception handling

Only two exception types exist — do not create new subclasses:

| Situation | Throw |
|---|---|
| Entity not found | `ResourceNotFoundException(entityName, fieldName, value)` → 404 |
| Business rule violation, duplicate data, bad token | `BadRequestException(message)` → 400 |

Concurrent update conflicts are handled automatically by `@Version` → 409.

### Testing patterns

- **Service unit tests**: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`. `@Value` fields must be set in `@BeforeEach` via `ReflectionTestUtils.setField(service, "fieldName", value)`.
- **Controller slice tests**: `@WebMvcTest(XController.class)` + `@Import({SecurityConfig.class, JwtConfig.class, SecurityBeans.class})` + `@MockitoBean` for each service dependency.
- **Full context test**: `@SpringBootTest` + `@ActiveProfiles("test")` loads `application-test.yml` (H2, mail stubs, `ddl-auto=create-drop`).

### Schema migrations

Manual SQL scripts in `backend/src/main/resources/db/migration/`. No Flyway configured. Apply to PostgreSQL before starting with a new schema version.

- `V2__auth_features.sql` — makes `password_hash` nullable; adds `provider`, `provider_account_id`, `password_reset_token`, `password_reset_expires`; adds unique constraint and partial index

## Frontend structure

- `src/app/components/header/` — Menubar (desktop), Drawer + TieredMenu (mobile), user avatar
- `src/app/components/home/` — Dashboard with summary cards + recent assets table
- `src/app/services/` — menu.service, asset.service, auth.service (all mock-based with simulated delay; no real HTTP calls yet)
- `src/app/models/` — TypeScript interfaces (MenuItem is recursive, Asset, User)
- PrimeNG 21: sidebar was renamed to Drawer; themes import from `@primeuix/themes`
- Angular signals for state; zoneless change detection throughout
