# API Gateway

Spring Cloud Gateway — public entry point.

- Port **8080**
- Routes `/api/notes/**`, `/api/users/**`, `/api/auth/**` → note-service
- Routes `/api/notifications/**` → notification-service
- **Redis** token-bucket rate limiting per client IP

See root [README](../README.md) for architecture rationale and trade-offs.
