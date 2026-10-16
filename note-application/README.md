# Note Application (Domain Service)

Core domain service: users, notes, JWT, RBAC, email verification & password reset (token hashing + attempt limits).

Publishes email events to **RabbitMQ**; does not send SMTP itself.

- Port: **8081** (behind API Gateway on **8080**)
- DB: PostgreSQL
- Cache / attempts: Redis
- Messaging: RabbitMQ

See root [README](../README.md) for full architecture, trade-offs, and security design.
