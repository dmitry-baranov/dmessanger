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


## Flutter Android local development

Local SDK/tool installations can live under `.tools/` and are ignored by git. In this workspace they were installed at:

```text
.tools/flutter
.tools/android-sdk
.tools/jdk-21
.tools/gradle
```

Backend checks with local JDK/Gradle:

```bash
JAVA_HOME="$PWD/.tools/jdk-21" PATH="$PWD/.tools/jdk-21/bin:$PATH" .tools/gradle/bin/gradle test
```

Mobile build and checks:

```bash
cd mobile
../.tools/flutter/bin/flutter pub get
../.tools/flutter/bin/flutter analyze
../.tools/flutter/bin/flutter test
../.tools/flutter/bin/flutter build apk --debug
```

Run from an Android emulator against the Docker backend:

```bash
../.tools/flutter/bin/flutter run \
  --dart-define=DMESSANGER_API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=DMESSANGER_WS_BASE_URL=ws://10.0.2.2:8080/ws
```

Use `alice/password` or `bob/password` for local login.

## Текущий backend-срез

Реализовано:

- Spring Boot Kotlin skeleton;
- health endpoints;
- login/refresh/logout/me для заранее созданных пользователей;
- dev seed пользователей `alice/password` и `bob/password`;
- подключение PostgreSQL/Redis через конфигурацию;
- Flyway initial schema;
- Dockerfile;
- Docker Compose для backend, PostgreSQL, Redis, MinIO, COTURN;
- REST API списка чатов;
- REST API истории и отправки сообщений;
- статусы delivered/read;
- dev direct chat между `alice` и `bob`.

Пока не реализовано:

- WebSocket realtime delivery;
- attachment upload/download API;
- WebRTC signaling;
- production E2EE/device key flow.

Следующий шаг — WebSocket realtime delivery для `message.created`, затем device/key bundle API и production E2EE.
