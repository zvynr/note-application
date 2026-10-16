# Notification Service

Async email worker.

- Port **8082**
- Consumes `notification.email.queue` from **RabbitMQ**
- Sends email via SMTP (Gmail app password)

Why a separate service + queue: note API must not block on SMTP; failures are isolated.

See root [README](../README.md) for architecture and security context.
