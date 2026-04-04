# Auth Service and API Gateway Working Guide

## 1. Purpose
This document explains how the Auth Service and API Gateway work together in the Vulnuris platform.

The security model is:
1. Auth Service issues signed JWTs.
2. API Gateway validates JWTs and enforces route-level RBAC.
3. Gateway forwards trusted identity context to downstream services.

## 2. Auth Service Working

### 2.1 Responsibilities
Auth Service is responsible for:
1. User registration.
2. User login.
3. Access and refresh token generation.
4. Token refresh.
5. Returning authenticated user profile information.
6. Persisting users and roles in PostgreSQL.

### 2.2 Exposed APIs
1. POST /api/auth/register
2. POST /api/auth/login
3. POST /api/auth/refresh
4. GET /api/auth/me

### 2.3 Data Model
Auth Service uses three core tables:
1. users
2. roles
3. user_roles

Entity relationship:
1. A user can have many roles.
2. A role can belong to many users.
3. user_roles is the join table.

Key design choices:
1. Username and email are unique.
2. Password is stored as password_hash.
3. Roles are enum based: ADMIN, ANALYST, VIEWER.

### 2.4 Registration Flow
1. Request is validated (username, email, password).
2. Username and email are normalized to lowercase.
3. Duplicates are checked.
4. Default role VIEWER is assigned.
5. Password is encoded with BCrypt.
6. User is saved.
7. Access and refresh tokens are immediately issued.

### 2.5 Login Flow
1. Credentials are validated by AuthenticationManager.
2. User is loaded from repository.
3. Account enabled check is performed.
4. New access and refresh tokens are generated.

### 2.6 Refresh Flow
1. Refresh token is validated.
2. Token type must be refresh.
3. Subject username is extracted.
4. User existence and enabled state are checked.
5. New access and refresh tokens are issued.

### 2.7 JWT Contents
Access token and refresh token are signed with HS256 and include:
1. subject: username
2. issuer: configured issuer
3. issuedAt and expiration
4. token_type: access or refresh
5. roles claim (present for access token use)

Configured validity:
1. Access token: 1800 seconds (30 minutes)
2. Refresh token: 2592000 seconds (30 days)

### 2.8 Security Configuration
Auth Service security is stateless:
1. Public endpoints:
   - POST /api/auth/register
   - POST /api/auth/login
   - POST /api/auth/refresh
   - /actuator/health
   - /actuator/info
2. All other endpoints require authentication.
3. JwtAuthenticationFilter extracts Bearer tokens and builds SecurityContext.
4. Structured 401 and 403 responses are returned by security handlers.
5. Global exception handler returns consistent API error payloads.

### 2.9 Bootstrap and Seeding
At startup:
1. Roles ADMIN, ANALYST, VIEWER are ensured.
2. Optional bootstrap admin can be created via config flags.

## 3. API Gateway Working

### 3.1 Responsibilities
API Gateway is responsible for:
1. Serving as the single entry point.
2. Routing requests to backend services.
3. Validating access tokens.
4. Enforcing route-level RBAC.
5. Returning unified 401 and 403 error payloads.
6. Adding trace and identity headers to forwarded requests.

### 3.2 Route Configuration
Configured routes:
1. /api/auth/** -> Auth Service
2. /api/events/** -> Event Service
3. /api/ingest/** -> Ingestion Service
4. /api/correlation/** -> Correlation Service
5. /api/report/** -> Report Service

Special compatibility route:
1. /api/ingest/** is rewritten to /logs/** so current IngestionService endpoints work without changing ingestion code.

### 3.3 Gateway JWT Validation
JwtAuthenticationGlobalFilter performs:
1. Public path bypass for login/register/refresh, OPTIONS, and health/info.
2. Bearer token presence check.
3. Signature and issuer validation.
4. Token type check (must be access token).
5. Role extraction from roles claim.
6. Route RBAC authorization decision.

If any step fails:
1. 401 for authentication failures.
2. 403 for authorization failures.

### 3.4 RBAC Policy in Gateway
Current policy:
1. /api/events with GET: ADMIN, ANALYST, VIEWER
2. /api/events with non-GET methods: ADMIN, ANALYST
3. /api/ingest/**: ADMIN, ANALYST
4. /api/correlation/**: ADMIN, ANALYST
5. /api/report/**: ADMIN, ANALYST, VIEWER
6. /api/auth/** (except public auth endpoints): authenticated roles allowed

### 3.5 Headers Added by Gateway
After successful validation, gateway forwards:
1. X-Auth-User: username
2. X-Auth-Roles: comma-separated roles
3. X-Trace-Id: correlation id

### 3.6 Traceability
TraceIdGlobalFilter behavior:
1. Reads incoming X-Trace-Id if present.
2. Generates a UUID if missing.
3. Stores trace id in exchange attributes.
4. Sends X-Trace-Id in response headers.

## 4. End-to-End Security Flow
1. Client calls POST /api/auth/login through gateway.
2. Request is routed to Auth Service (public endpoint).
3. Auth Service returns access_token and refresh_token.
4. Client calls protected APIs with Authorization: Bearer access_token.
5. Gateway validates token and checks roles.
6. If allowed, request is forwarded to target service.
7. Target service receives trusted identity headers from gateway.

If access token expires:
1. Client calls POST /api/auth/refresh with refresh_token.
2. Auth Service validates and returns new token pair.

## 5. Critical Configuration Contract
These values must match in Auth Service and Gateway:
1. JWT secret
2. JWT issuer

If they mismatch, gateway will reject all tokens with 401.

## 6. Operational Notes
1. Gateway runs on port 8080 (frontend entry point).
2. Auth Service runs on port 8091 by default.
3. Auth persistence uses PostgreSQL with JPA update mode.
4. For production, set secure secrets and database credentials through environment variables.
5. Consider adding refresh token rotation and revocation in future hardening.
