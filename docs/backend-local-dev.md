# Backend local development

## Запуск через Docker Compose

```bash
cp deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build
```

Backend будет доступен на `http://localhost:8080`.

Проверки:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"alice","password":"password"}'
```

MinIO Console: `http://localhost:9001`.

## Текущий backend-срез

Реализовано:

- Spring Boot Kotlin skeleton;
- health endpoints;
- login/refresh/logout/me для заранее созданных пользователей;
- dev seed пользователей `alice/password` и `bob/password`;
- подключение PostgreSQL/Redis через конфигурацию;
- Flyway initial schema;
- Dockerfile;
- Docker Compose для backend, PostgreSQL, Redis, MinIO, COTURN.

Пока не реализовано:

- бизнес-API чатов и сообщений;
- WebSocket;
- WebRTC signaling.

Следующий шаг — device/key bundle API для E2EE и затем базовые chats/messages API.
