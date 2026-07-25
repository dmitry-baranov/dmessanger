# dmessanger TODO

## Сейчас работает

- Android Flutter app собирается в debug APK.
- Login/refresh/logout через backend.
- Secure storage для access/refresh token.
- Список чатов через `GET /api/v1/chats`.
- Dev direct chat между `alice` и `bob` создаётся при seed.
- История сообщений через `GET /api/v1/chats/{chatId}/messages`.
- Отправка сообщений через `POST /api/v1/chats/{chatId}/messages`.
- Статусы `delivered/read` через REST.

## Проверено

- Backend tests проходят:

```bash
JAVA_HOME="$PWD/.tools/jdk-21" \
PATH="$PWD/.tools/jdk-21/bin:$PATH" \
.tools/gradle/bin/gradle test
```

- Flutter checks проходят:

```bash
cd mobile
../.tools/flutter/bin/flutter analyze
../.tools/flutter/bin/flutter test
../.tools/flutter/bin/flutter build apk --debug
```

- Docker Compose smoke проверен на localhost:
  - Alice видит чат `Bob`;
  - Alice отправляет сообщение;
  - Bob видит чат `Alice`;
  - Bob получает сообщение;
  - `delivered/read` работают.

## Запуск локально

Backend:

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

Flutter emulator run:

```bash
cd mobile
../.tools/flutter/bin/flutter run \
  --dart-define=DMESSANGER_API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=DMESSANGER_WS_BASE_URL=ws://10.0.2.2:8080/ws
```

Dev users:

- `alice/password`
- `bob/password`

## Осталось сделать

1. WebSocket realtime delivery.
   - Backend `/ws` auth.
   - Событие `message.created` после отправки сообщения.
   - Flutter reconnect lifecycle.
   - Автообновление списка чатов и открытого чата.

2. Device/key bundle API.
   - Регистрация device public keys.
   - Signed pre-key и one-time pre-keys.
   - Получение key bundle другого пользователя.

3. Production E2EE для сообщений.
   - Убрать временный `dev-plaintext-base64` flow.
   - Шифровать сообщение на клиенте до отправки.
   - Backend хранит только настоящий ciphertext.
   - Plaintext не логируется и не передаётся на backend.

4. Локальная база Flutter.
   - Drift schema для chats/messages/outbox.
   - Offline queue исходящих сообщений.
   - Кэш истории.

5. Медиа-вложения.
   - Backend upload intent.
   - Pre-signed upload/download URLs.
   - Локальное шифрование фото/видео перед upload.
   - Preview и progress в Flutter.

6. WebRTC звонки.
   - Backend call signaling.
   - Incoming/outgoing call screens.
   - `flutter_webrtc` peer connection.
   - TURN config и завершение звонка.

7. Hardening.
   - Rate limit login.
   - Интеграционные тесты REST/WebSocket.
   - Production TLS/WSS config.
   - Security checklist перед релизом.
