# Note Platform

<p align="left">
  <img src="https://img.shields.io/badge/Java-8-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring_Boot-2.7.18-brightgreen?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring_Cloud-2021.0.9-green?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"/>
  <img src="https://img.shields.io/badge/RabbitMQ-3.13-orange?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
</p>

Backend platform for note management, built as a **small multi-service system** with an API Gateway, async notifications, caching, and hardened auth flows.

Designed as a **portfolio / learning project** that demonstrates practical system design choices—not just a single Spring Boot CRUD app.

---

## Architecture

```text
                         ┌─────────────────┐
                         │  Client / cURL  │
                         └────────┬────────┘
                                  │ :8080
                                  ▼
                    ┌─────────────────────────────┐
                    │        API Gateway          │
                    │  • routing                  │
                    │  • CORS                     │
                    │  • Redis rate limit (IP)    │
                    └─────────────┬───────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │ /api/notes|users|auth                 │ /api/notifications
              ▼                                       ▼
   ┌──────────────────────┐              ┌────────────────────────┐
   │    note-service      │              │  notification-service  │
   │    :8081             │              │  :8082                 │
   │  • JWT + RBAC        │   publish    │  • consume RabbitMQ    │
   │  • Notes / Users     │ ───────────► │  • send email (SMTP)   │
   │  • Email verify/reset│   RabbitMQ   └───────────┬────────────┘
   │  • Redis cache       │                          │
   └──────────┬───────────┘                          ▼
              │                                    Gmail
              ▼
         PostgreSQL
              │
         Redis (cache + attempt limits)
```

| Component | Port | Responsibility |
|-----------|------|----------------|
| **api-gateway** | 8080 | Public entry: route + rate limit |
| **note-application** | 8081 | Domain API: users, notes, auth, tokens |
| **notification-service** | 8082 | Email worker (async consumer) |
| **PostgreSQL** | 5432 | Persistent data |
| **Redis** | 6379 | Gateway rate limit, note cache, attempt limits |
| **RabbitMQ** | 5672 / 15672 | Async email events (+ management UI) |

---

## Tech Stack

- **Core & Framework**: Java 8, Spring Boot 2.7.18, Spring Cloud 2021.0.9 (Spring Cloud Gateway)
- **Database & Persistence**: PostgreSQL 16, Spring Data JPA / Hibernate
- **Caching & Rate Limiting**: Redis 7, Spring Data Redis (Blocking & Reactive), Spring Cloud Gateway RedisRateLimiter
- **Messaging / Async**: RabbitMQ 3.13 (AMQP), Spring AMQP
- **Security & Auth**: Spring Security, JSON Web Token (JWT), BCrypt Password Hashing
- **Documentation**: Springfox Swagger 2 (Swagger UI)
- **Utilities & Build**: Maven, Project Lombok, ModelMapper
- **Containerization**: Docker, Docker Compose, Eclipse Temurin JRE 8

---

## Why this architecture?

### 1. API Gateway in front

**Reason:** One public entry point for routing, CORS, and abuse control (rate limiting), without mixing that into domain code.

**Trade-off:**

| Pros | Cons |
|------|------|
| Clear edge layer | Extra hop / latency |
| Rate limit applies to all APIs | Gateway is a critical dependency |
| Backend ports can stay internal | More moving parts to operate |

### 2. Split notification into its own service

**Reason:** Sending email is a different concern (SMTP, retries, credentials). The note API should not block on mail servers.

**Trade-off:**

| Pros | Cons |
|------|------|
| note-service stays focused | Network / ops complexity |
| notification can scale alone | Eventual consistency (email arrives later) |
| Failure isolation (SMTP down ≠ whole API down) | Need messaging infrastructure |

### 3. RabbitMQ instead of HTTP sync or Kafka

**Reason:** Forgot-password / verify-email are **fire-and-forget jobs**. The API only needs to enqueue work.

| Option | Decision |
|--------|----------|
| **HTTP sync** to notification | ❌ Blocks request; couples availability to SMTP |
| **Redis Pub/Sub** | ❌ Messages lost if no consumer is online |
| **Kafka** | ❌ Overkill for low-volume notification jobs (heavy ops, replay log not needed) |
| **RabbitMQ** | ✅ Durable queue, simple worker model, retries-friendly |

**Trade-off vs Kafka:** less throughput/replay power; much simpler for this domain.  
**Trade-off vs Redis Pub/Sub:** slightly more infrastructure; messages survive consumer downtime.

### 4. Redis for rate limit + cache + attempt limits

**Reason:** Shared, fast store for cross-instance state.

| Use | Why Redis |
|-----|-----------|
| Gateway rate limit | Works across multiple gateway instances |
| Note list cache | Fewer DB hits on read-heavy paths |
| Auth attempt limits | Short-lived counters with TTL |

**Trade-off:** Redis becomes shared infrastructure; cache invalidation must be correct on write.

### 5. note-service still owns auth domain

**Reason:** Users, JWT, RBAC, and notes share the same transactional boundary. Splitting “Auth service” too early adds distributed transactions without clear benefit at this scale.

**Trade-off:** note-service is larger; acceptable until auth traffic or team boundaries justify a split.

---

## Security design

### Authentication & authorization

- **JWT** (stateless) after login  
- **RBAC:** `USER` vs `ADMIN`  
- **Ownership checks:** users only access their own notes (admins can access all)  
- **Password:** BCrypt  

### Email verification (registration)

```text
Register → emailVerified=false → send verify link (async)
        → user cannot login until GET /api/users/verify?token=...
```

- Proves **ownership of the email**, not just “email field exists”.

### Password reset

```text
POST /forgot → always same response (anti email enumeration)
            → if email exists: store hashed token, publish email event
GET  /reset?token= → validate
PUT  /resetPass    → set new password, clear token
```

### Token storage (important)

| Sent to user (email link) | Stored in database |
|---------------------------|--------------------|
| **Raw** high-entropy token | **SHA-256 hash** only |

If the database is leaked, attackers cannot reuse tokens without the raw value from the email.

Tokens are generated with `SecureRandom` (32 bytes, URL-safe).

### Short TTLs

| Token | Lifetime |
|-------|----------|
| Email verification | **60 minutes** (configurable) |
| Password reset | **15 minutes** (configurable) |

### Attempt limiting (Redis)

Sensitive actions limited (default **5 attempts / 15 minutes**):

- Forgot-password (per email)  
- Verify email (per token)  
- Reset validate / update (per token)  

Plus **gateway IP rate limiting** (Redis token-bucket) on the edge.

### Anti email enumeration

Forgot-password always returns:

> *If an account with that email exists, a password reset link has been sent.*

Whether or not the email is registered.

### Public URL for links

Verification/reset links use `app.public-url` (gateway), not the internal service host—so links work behind Docker/proxy.

---

## Main API flows (via gateway `:8080`)

| Flow | Steps |
|------|--------|
| **Register** | `POST /api/users/register` → email verify link → `GET /api/users/verify?token=` → login |
| **Login** | `POST /api/auth/login` → JWT (requires verified email) |
| **Notes** | CRUD + pagination/filter under `/api/notes/**` (RBAC + ownership) |
| **Forgot password** | `POST /api/users/forgot?email=` → email → reset endpoints |

Always call the **gateway**, not internal ports.

---

## Run

### Docker Compose (recommended)

```bash
# optional: real Gmail app password for email
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=your-app-password

docker compose up --build
```

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | Public API (gateway) |
| http://localhost:15672 | RabbitMQ management (`guest`/`guest`) |
| http://localhost:8080/swagger-ui.html | API docs (proxied) |

### Local modules

1. PostgreSQL + Redis + RabbitMQ up  
2. `notification-service` (8082)  
3. `note-application` (8081)  
4. `api-gateway` (8080)  

---

## Configuration highlights

```properties
# note-application
app.public-url=http://localhost:8080
security.token.verify-minutes=60
security.token.reset-minutes=15
security.attempts.max=5
security.attempts.window-minutes=15
```

```yaml
# api-gateway rate limit (Redis token bucket)
rate-limit.replenish-rate: 10   # tokens/sec
rate-limit.burst-capacity: 20
```

---

## Project structure

```text
├── api-gateway/           # Spring Cloud Gateway + Redis rate limiter
├── note-application/      # Domain service (JWT, RBAC, notes, tokens)
├── notification-service/  # Email consumer (RabbitMQ → SMTP)
├── docker-compose.yml     # Full stack
└── README.md              # This file
```

---


## License

Educational / portfolio use.
