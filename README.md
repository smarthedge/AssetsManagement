# Assets Management System

Full-stack enterprise asset tracking application with audit trails, role-based access control, and responsive design.

## Project Overview

Manage organizational assets — laptops, monitors, servers, peripherals, and more — with full CRUD operations, soft delete, optimistic locking, and comprehensive audit logging. The backend serves REST APIs secured with OAuth 2.0 / JWT; the frontend provides a responsive Angular 21 UI with PrimeNG components and Tailwind CSS.

### Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 21 (standalone, zoneless), PrimeNG 21, Tailwind CSS v4, Vitest |
| Backend | Spring Boot 4.0.6, JDK 25, Spring Security (OAuth2 Resource Server), JPA/Hibernate |
| Security | RSA 2048-bit JWT (Nimbus), BCrypt password encoding, stateless sessions |
| Database | PostgreSQL 16 (primary), H2 (dev/test) |
| Tests | JUnit 5 + Mockito (backend), Vitest (frontend) |
| Docs | SpringDoc OpenAPI 2.7 (Swagger) |

---

## Repository Structure

```
AssetsManagement/
├── database/
│   └── scripts/
│       └── init-ddl.sql          # PostgreSQL schema + seed data
├── backend/                      # Spring Boot 4.0.6 (Maven)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd           # Maven wrapper (no Maven install needed)
│   └── src/
│       ├── main/java/com/assetsmanagement/
│       │   ├── AssetsManagementApplication.java
│       │   ├── audit/            # AuditAwareImpl (SecurityContext → audit fields)
│       │   ├── config/           # SecurityConfig, JwtConfig, OpenApiConfig
│       │   ├── controller/       # Auth, Asset, User, Role, Menu controllers
│       │   ├── dto/              # Request/Response records, PageResponse<T>
│       │   ├── entity/           # BaseEntity, User, Role, Asset (JPA)
│       │   ├── exception/        # GlobalExceptionHandler + custom exceptions
│       │   ├── repository/       # Spring Data JPA repositories
│       │   └── service/          # Business logic (CRUD, JWT, menu)
│       └── test/                 # JUnit 5 + Mockito tests (21 tests)
├── frontend/                     # Angular 21 (standalone components)
│   ├── package.json
│   ├── angular.json
│   ├── proxy.conf.json           # Dev proxy /api → localhost:8080
│   └── src/
│       ├── main.ts
│       └── app/
│           ├── app.ts            # App shell: header + main + footer
│           ├── app.config.ts     # Zoneless, PrimeNG Aura theme, router
│           ├── app.routes.ts     # Route definitions
│           ├── components/
│           │   ├── header/       # Menubar (desktop) / Drawer+TieredMenu (mobile)
│           │   ├── footer/       # Copyright footer
│           │   └── home/         # Dashboard: summary cards + recent assets
│           ├── services/         # Asset, Auth, Menu services (mock data)
│           ├── models/           # TypeScript interfaces
│           └── mock/             # 10 sample assets, menu tree, users
├── CLAUDE.md                     # AI coding assistant guidance
└── README.md                     # This file
```

---

## Architecture

### Database Schema

Four tables with BIGINT primary keys, six audit columns, soft delete via `status` boolean, and `version` column for optimistic locking:

| Table | Key Columns | Notes |
|---|---|---|
| `users` | id, username, email, password_hash | Unique on username + email |
| `roles` | id, name, description | Seed: ROLE_ADMIN, ROLE_USER |
| `user_roles` | user_id, role_id | Composite PK, CASCADE deletes |
| `assets` | id, name, category, serial_number, value | Unique serial_number |

**Audit columns** (on each table): `created_by_user_id`, `created_by_username`, `created_datetime`, `last_changed_by_user_id`, `last_changed_by_username`, `last_changed_datetime`.

**Soft delete**: All repository queries filter `status = true`. Deleting sets `status = false` — rows are never physically removed.

**Optimistic locking**: `@Version Integer version` prevents lost updates. On conflict, the API returns HTTP 409.

### Backend API

All endpoints under `/api`. Public: `/api/auth/**`. All others require authentication. CUD operations require `ROLE_ADMIN`.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Authenticate, returns JWT |
| GET | `/api/menu` | Authenticated | Hierarchical menu tree |
| GET | `/api/assets?page=&size=&category=` | Authenticated | Paginated asset list |
| GET | `/api/assets/{id}` | Authenticated | Single asset |
| POST | `/api/assets` | ADMIN | Create asset |
| PUT | `/api/assets/{id}` | ADMIN | Update asset (checks @Version) |
| DELETE | `/api/assets/{id}` | ADMIN | Soft-delete asset |
| GET | `/api/users` | ADMIN | Paginated user list |
| POST | `/api/users` | ADMIN | Create user |
| PUT | `/api/users/{id}` | ADMIN | Update user |
| DELETE | `/api/users/{id}` | ADMIN | Soft-delete user |
| GET | `/api/roles` | ADMIN | Paginated role list |

**JWT flow**: Client POSTs credentials → receives signed RS256 JWT (60-min expiry) → attaches `Authorization: Bearer <token>` to subsequent requests. OAuth2 Resource Server validates signature and roles.

### Frontend Component Tree

```
AppComponent (shell)
├── HeaderComponent
│   ├── Desktop: p-menubar (dynamic model from MenuService)
│   │   └── Admin items pushed right via styleClass="ml-auto"
│   ├── User avatar + username (far right)
│   └── Mobile: hamburger → p-drawer + p-tieredMenu
├── <router-outlet> (inside flex-1 overflow-y-auto main)
│   └── HomeComponent (Dashboard)
│       ├── 3 summary cards (Total Assets, Total Value, Categories)
│       └── Recent Assets table (top 5)
└── FooterComponent
```

**Responsive behavior**: Menubar is `hidden lg:flex`; hamburger button is `lg:hidden`. Drawer slides in from the left on mobile.

**State management**: Uses Angular signals (`signal()`) with zoneless change detection. Services return `Observable<T>` with simulated network delay (200–500ms). Auth state held in `BehaviorSubject`-style signals.

---

## Installation & Setup

### Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java JDK | 25+ | `java --version` |
| Node.js | 24+ | `node --version` |
| npm | 11+ | `npm --version` |
| PostgreSQL | 16+ | `psql --version` |
| Angular CLI | 21+ | `npx ng version` |

### 1. Clone & Install

```bash
git clone <repo-url>
cd AssetsManagement

# Frontend dependencies
cd frontend
npm install
cd ..

# Backend uses Maven wrapper — no Maven install needed
```

### 2. Database Setup

Create the PostgreSQL database and run the init script:

```bash
# Create database
psql -U postgres -c "CREATE DATABASE assets_management;"

# Run DDL + seed data
psql -U postgres -d assets_management -f database/scripts/init-ddl.sql
```

This creates all tables, indexes, and seed data (admin user with password `admin123`, 10 sample assets).

### 3. Configuration

**Backend** (`backend/src/main/resources/application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assets_management
    username: postgres
    password: ${DB_PASSWORD:postgres}
```

Set the database password via environment variable or edit the default:
```bash
export DB_PASSWORD=your_password    # Linux/macOS
set DB_PASSWORD=your_password       # Windows
```

**Frontend** (`frontend/proxy.conf.json`):
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

### 4. Run the Application

**Start the backend** (terminal 1):
```bash
cd backend

# With PostgreSQL (production profile)
./mvnw spring-boot:run

# Or with H2 in-memory database (dev profile — no PostgreSQL needed)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend starts on **http://localhost:8080**. Swagger UI at **http://localhost:8080/swagger-ui.html**.

**Start the frontend** (terminal 2):
```bash
cd frontend
npm start
```

Frontend starts on **http://localhost:4200**. API calls proxy to `:8080`.

### 5. Login

Open http://localhost:4200. The app loads with mock data in the UI.

To authenticate against the real backend:
- **Username**: `admin`
- **Password**: `admin123`

---

## Running Tests

### Backend Tests (21 tests)

```bash
cd backend
./mvnw test                              # Run all tests
./mvnw test -Dtest=AssetServiceTest      # Run a single test class
```

Tests use H2 in-memory database via the `test` profile. Covers:
- Service layer: CRUD operations, optimistic locking, soft delete, not-found scenarios
- Controller layer: authentication, authorization (ADMIN vs USER roles), validation

### Frontend Tests (26 tests)

```bash
cd frontend
npm test                    # Run all tests (Vitest)
npx ng test --watch=false   # Run once (no watch)
```

Covers:
- Services: asset CRUD (7 tests), auth login/logout (4 tests), menu items (4 tests)
- Components: app shell (2 tests), home dashboard (4 tests), header (5 tests)

---

## Production Build

```bash
# Backend
cd backend
./mvnw package -DskipTests
# JAR: backend/target/assets-management-0.0.1-SNAPSHOT.jar

# Frontend
cd frontend
npx ng build
# Output: frontend/dist/assets-management-ui/
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| **Standalone components** | Angular 21 default — no NgModule boilerplate |
| **Zoneless change detection** | Better performance, signals-based reactivity |
| **Soft delete via status** | Preserves audit trail; all queries filter `status = true` |
| **@Version optimistic locking** | JPA built-in; returns HTTP 409 on concurrent modification |
| **Separate DTOs** | API contracts decoupled from JPA entities |
| **Menubar over MegaMenu** | Simple nested menu data fits `MenuItem[]` better than `MegaMenuItem[][]` |
| **Dev profile with H2** | Enables backend development without PostgreSQL installation |
| **Mock services (frontend)** | UI development and testing without a running backend |
| **Maven wrapper (mvnw)** | No Maven installation required |

---

## License

MIT
