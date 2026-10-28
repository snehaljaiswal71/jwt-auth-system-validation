# JWT Auth System

Spring Boot 4 REST API for JWT based authentication with in-memory users, OTP verification, country restricted sign-up, role based settings access and PostgreSQL backed product management.

## Stack

- Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA
- PostgreSQL (products), in-memory store (users, OTPs, revoked tokens)
- JJWT 0.13 (HS512 signed tokens with `jti`, `iss`, `sub`, `iat`, `nbf`, `exp`)
- BCrypt (cost 12) password hashing
- springdoc-openapi (Swagger UI)

## Run

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Postman collection: `postman/jwt-auth-system.postman_collection.json`

### Configuration

| Env variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/authsystem`, `postgres`, `postgres` | PostgreSQL connection |
| `JWT_SECRET` | development key | Base64 HMAC key, at least 64 bytes. **Override in any real deployment** |
| `JWT_EXPIRATION` | `30m` | Token lifetime |
| `OTP_EXPOSE_IN_RESPONSE` | `true` | Returns the OTP in the sign-up/resend response because no email gateway is configured |
| `GEO_FAIL_OPEN` | `false` | Allow sign-up when the country lookup fails |
| `GEO_TRUST_FORWARDED_HEADER` | `false` | Use `X-Forwarded-For` as the client IP (enable only behind a trusted proxy, or for testing other countries) |

Blocked countries are configured in `app.geo.blocked-countries` (ISO codes; defaults include SY, AF, IR, KP, CU, SD, RU, BY).

## Flow

1. `POST /api/auth/signup` - validates input, resolves the caller's country from their IP via ip-api.com (for a local/private IP it resolves the server's public location), blocks restricted countries, stores the user unverified and issues a 6 digit OTP (5 minute TTL, 5 attempts).
2. `POST /api/auth/verify-otp` - activates the account. `POST /api/auth/resend-otp` issues a new OTP.
3. `POST /api/auth/login` - returns a Bearer JWT. Invalid credentials respond `303` to `/api/auth/error`, which returns `Login Failed`.
4. `GET /api/dashboard` - returns `Welcome <username>`.
5. `GET /api/settings` - Administrator only; Standard User receives `403`.
6. `POST /api/auth/logout` - returns `<username> Logged Out` and revokes the token; any later call with it gets `401`.

### Validation rules

- Username: 8-50 characters, letters, digits, `.`, `_`, `-`
- Email: valid address containing both `@` and `_`
- Password: 8-12 characters, at least one uppercase, lowercase, digit and special character, no spaces
- User type: `Standard User` or `Administrator`

## Products

All product endpoints require a valid token.

| Method | Path | Description |
|---|---|---|
| POST | `/api/products` | Create |
| GET | `/api/products` | List with `name` (partial), `category` (exact), `minPrice`, `maxPrice`, `page`, `size` (max 100), `sort=<field>,<asc\|desc>` |
| GET | `/api/products/{id}` | Read |
| PUT | `/api/products/{id}` | Replace |
| DELETE | `/api/products/{id}` | Delete |

Sortable fields: `id`, `name`, `category`, `price`, `quantity`, `createdAt`, `updatedAt`.

## Errors

Every error returns:

```json
{ "timestamp": "2026-10-28T10:15:30Z", "status": 400, "code": "VALIDATION_FAILED", "message": "Request validation failed", "path": "/api/auth/signup", "fieldErrors": { "email": "Email must be a valid address containing '@' and '_'" } }
```

## Tests

```bash
./mvnw verify
```

Integration tests run against in-memory H2 with the geolocation lookup mocked.
