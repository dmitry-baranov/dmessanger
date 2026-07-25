# План backend-разработки

## Цель backend

Один backend-монолит, который обслуживает Android-клиенты, хранит только серверные метаданные и шифротекст, управляет авторизацией, чатами, доставкой сообщений, загрузкой файлов и signaling для WebRTC-звонков.

## Структура проекта

```text
server/
├── build.gradle.kts
├── Dockerfile
├── src/main/kotlin/
│   └── app/dmessanger/
│       ├── DmessangerApplication.kt
│       ├── auth/
│       ├── users/
│       ├── devices/
│       ├── chats/
│       ├── messages/
│       ├── attachments/
│       ├── realtime/
│       ├── calls/
│       ├── crypto/
│       └── admin/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
└── src/test/kotlin/
```

## Docker Compose

```text
deploy/
├── docker-compose.yml
├── .env.example
└── minio/
```

Сервисы:

- `backend` — основное приложение.
- `postgres` — постоянные данные.
- `redis` — realtime/session/cache.
- `minio` — encrypted attachments.
- `coturn` — STUN/TURN для WebRTC.


## Текущий backend-срез

Реализовано:

- health endpoints;
- login/refresh/logout/me;
- seed пользователей `alice/password` и `bob/password`;
- seed direct chat между первыми двумя dev-пользователями;
- `GET /api/v1/chats`;
- `POST /api/v1/chats/direct`;
- `GET /api/v1/chats/{chatId}`;
- `GET /api/v1/chats/{chatId}/messages`;
- `POST /api/v1/chats/{chatId}/messages`;
- `POST /api/v1/messages/{messageId}/delivered`;
- `POST /api/v1/messages/{messageId}/read`.

Пока не реализовано:

- WebSocket realtime delivery;
- attachment upload/download API;
- device/key bundle API;
- production E2EE;
- WebRTC signaling.

Важно: текущая отправка сообщений поддерживает временный dev mode `dev-plaintext-base64`, чтобы Flutter UI мог показать текст до внедрения E2EE. Production flow должен отправлять только настоящий ciphertext.

## Backend-модули

### Auth

Функции: login/password авторизация, access/refresh token, refresh, logout, хранение refresh token hash, rate limit попыток входа.

Пароли хранить через Argon2id. Plaintext password нигде не логировать.

### Users

Функции: список пользователей, профиль текущего пользователя, создание пользователей через seed/config/CLI, включение/отключение пользователя. Регистрации через публичный API нет.

### Devices

Функции: регистрация устройства после первого входа, хранение public identity key, signed pre-key, one-time pre-keys, выдача public key bundle другим пользователям.

Backend хранит только публичные ключи и не должен получать приватные ключи.

### Chats

Функции: личный чат, групповой чат, список чатов пользователя, участники чата, роли owner/member, добавление/удаление участников.

### Messages

Функции: сохранение encrypted message payload, история с pagination, realtime-доставка через WebSocket, статусы sent/delivered/read, idempotency key для повторной отправки после offline.

Backend не валидирует plaintext, только техническую структуру payload.

### Attachments

Функции: upload intent, pre-signed upload/download URL, metadata, лимиты размера файлов. Файл шифруется на клиенте до загрузки.

### Realtime

Функции: WebSocket auth, подписка на события чатов пользователя, доставка online-клиентам, сохранение событий для offline-клиентов, presence через Redis.

### Calls

Функции: создать звонок, принять/отклонить/завершить, signaling SDP offer/answer, ICE candidates, состояние участников, timeout входящего звонка.

Backend не получает media stream. Он только координирует signaling.

## REST API draft

### Auth

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/me
```

### Devices and keys

```text
POST /api/v1/devices
GET  /api/v1/users/{userId}/devices
POST /api/v1/devices/{deviceId}/prekeys
GET  /api/v1/users/{userId}/key-bundle
```

### Chats

```text
GET    /api/v1/chats
POST   /api/v1/chats/direct
POST   /api/v1/chats/group
GET    /api/v1/chats/{chatId}
POST   /api/v1/chats/{chatId}/members
DELETE /api/v1/chats/{chatId}/members/{userId}
```

### Messages

```text
GET  /api/v1/chats/{chatId}/messages
POST /api/v1/chats/{chatId}/messages
POST /api/v1/messages/{messageId}/delivered
POST /api/v1/messages/{messageId}/read
```

### Attachments

```text
POST /api/v1/attachments/upload-intent
POST /api/v1/attachments/{attachmentId}/complete
GET  /api/v1/attachments/{attachmentId}/download-url
```

### Calls

```text
POST /api/v1/calls
POST /api/v1/calls/{callId}/accept
POST /api/v1/calls/{callId}/reject
POST /api/v1/calls/{callId}/end
```

## WebSocket events draft

Endpoint:

```text
GET /ws
```

Client -> server:

```text
message.send
message.delivered
message.read
typing.started
typing.stopped
call.offer
call.answer
call.ice_candidate
call.reject
call.end
presence.ping
```

Server -> client:

```text
message.created
message.updated
message.delivered
message.read
typing.started
typing.stopped
call.incoming
call.offer
call.answer
call.ice_candidate
call.participant_joined
call.participant_left
call.ended
presence.updated
```

## База данных

Предварительные таблицы:

- `users`
- `user_credentials`
- `devices`
- `device_prekeys`
- `sessions`
- `chats`
- `chat_members`
- `messages`
- `message_recipients`
- `attachments`
- `calls`
- `call_participants`
- `outbox_events`

Важные поля `messages`: `id`, `chat_id`, `sender_user_id`, `sender_device_id`, `client_message_id`, `ciphertext`, `encryption_metadata`, `created_at`.

Важные поля `attachments`: `id`, `message_id`, `object_key`, `encrypted_file_key_payload`, `mime_type`, `size_bytes`, `sha256`, `created_at`.

Важные поля `devices`: `id`, `user_id`, `device_name`, `identity_public_key`, `signed_prekey_public`, `signed_prekey_signature`, `created_at`, `last_seen_at`.

## Конфигурация

Через env:

```text
SERVER_PORT
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD
REDIS_URL
S3_ENDPOINT
S3_BUCKET
S3_ACCESS_KEY
S3_SECRET_KEY
JWT_ACCESS_SECRET
JWT_REFRESH_SECRET
CORS_ALLOWED_ORIGINS
TURN_PUBLIC_URL
```

Секреты не коммитить. В git хранить только `.env.example`.

## Порядок реализации backend

1. Скелет Spring Boot приложения.
2. Dockerfile.
3. Docker Compose с PostgreSQL, Redis, MinIO, COTURN.
4. Миграции БД.
5. Healthcheck endpoint.
6. Auth: login/refresh/logout.
7. Seed заранее созданных пользователей.
8. Device API и key bundle API.
9. Chat API.
10. Message API.
11. WebSocket endpoint.
12. Realtime delivery.
13. Attachment upload/download через MinIO.
14. Call signaling.
15. Rate limit и audit logs.
16. Интеграционные тесты.

## Definition of Done для backend MVP

- `docker-compose up` поднимает backend и зависимости.
- Можно создать/засидить пользователей без публичной регистрации.
- Login возвращает рабочую сессию.
- Клиент получает список чатов.
- Клиент отправляет encrypted message payload.
- Второй клиент получает событие через WebSocket.
- Файлы загружаются и скачиваются через pre-signed URLs.
- WebRTC signaling проходит через backend.
- Backend не хранит plaintext сообщений и приватные ключи.
