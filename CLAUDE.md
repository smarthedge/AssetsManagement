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
./mvnw spring-boot:run                    # Start on port 8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # Dev mode (H2)
./mvnw test                                # Run all tests
./mvnw test -Dtest=AssetServiceTest        # Run single test class
```

Swagger UI: http://localhost:8080/swagger-ui.html

### Frontend (`frontend/`)
```bash
npm start                  # Start on port 4200 (dev server with proxy to :8080)
npm test                   # Run unit tests (Vitest)
npx ng build               # Production build to dist/
npx ng test --watch=false  # Run tests once
```

## Architecture

### Backend packages (`com.assetsmanagement`)
- `entity/` — BaseEntity (@MappedSuperclass with audit fields, @Version), User, Role, Asset
- `dto/` — Separate request/response records; MenuItemResponse has recursive `List<MenuItemResponse> items`
- `repository/` — Spring Data JPA; all queries filter `status=true` (soft delete)
- `service/` — Business logic with SLF4J logging; update operations check @Version for optimistic locking
- `controller/` — REST endpoints with @PreAuthorize, Swagger @Operation
- `config/` — JwtConfig (RSA 2048 key pair), SecurityConfig (OAuth2 Resource Server, stateless), OpenApiConfig
- `exception/` — GlobalExceptionHandler maps 404/400/409/401/403
- `audit/` — AuditAwareImpl extracts username from SecurityContext

### Frontend structure
- `src/app/components/header/` — Menubar (desktop), Drawer + TieredMenu (mobile), user avatar
- `src/app/components/home/` — Dashboard with summary cards + recent assets table
- `src/app/services/` — menu.service, asset.service, auth.service (all mock-based with simulated delay)
- `src/app/mock/` — mock-data.ts with sample assets, users, menu tree
- `src/app/models/` — TypeScript interfaces (MenuItem recursive, Asset, User)

### Key patterns
- All entities extend BaseEntity (status, version, 6 audit columns, @PrePersist/@PreUpdate)
- Soft delete: set `status=false`; all repository queries use `findByStatusTrue`
- Optimistic locking: @Version field returns HTTP 409 on conflict
- Frontend uses Angular signals (zoneless change detection)
- Backend MenuService returns hierarchical menu; Frontend maps to PrimeNG MenuItem[]
- PrimeNG 21: sidebar renamed to drawer, themes in @primeuix/themes
