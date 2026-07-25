# dmessanger mobile

Android Flutter client for dmessanger.

## Current slice

Implemented:

- Android-only Flutter project;
- Material 3 theme and go_router navigation;
- login screen integrated with `POST /api/v1/auth/login`;
- refresh-token restore integrated with `POST /api/v1/auth/refresh`;
- logout integrated with `POST /api/v1/auth/logout`;
- bearer-token Dio client;
- refresh/access token storage via `flutter_secure_storage`;
- chat list screen wired to `GET /api/v1/chats`;
- chat screen wired to messages REST endpoints;
- WebSocket client shell for future `/ws` endpoint;
- settings/diagnostics screen.

The backend currently implements auth, chat list, message history, message send, and delivered/read status REST endpoints. Realtime, attachment, and call UI still show explicit pending-backend states instead of local mocks.

## Local commands

From repository root:

```bash
cd mobile
../.tools/flutter/bin/flutter pub get
../.tools/flutter/bin/flutter analyze
../.tools/flutter/bin/flutter test
../.tools/flutter/bin/flutter build apk --debug
```

Run against the local Docker backend from an Android emulator:

```bash
../.tools/flutter/bin/flutter run \
  --dart-define=DMESSANGER_API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=DMESSANGER_WS_BASE_URL=ws://10.0.2.2:8080/ws
```

For a physical Android device on the same network, replace `10.0.2.2` with the host machine IP address.

Dev users seeded by the backend are `alice/password` and `bob/password`.
