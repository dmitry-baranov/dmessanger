# Roadmap реализации

## Принцип разработки

Двигаемся итерациями. Сначала строим работающий вертикальный срез без полной криптографии и звонков, затем усиливаем безопасность и добавляем сложные функции. Это снижает риск застрять на WebRTC/E2EE до появления базового продукта.

Важно: временные упрощения должны быть явно помечены и не должны попасть в production.

## Итерация 0 — подготовка репозитория

Результат:

- структура монорепы;
- базовый README;
- документация архитектуры;
- базовый `.gitignore`;
- dev-конфигурация.

Задачи:

- создать `mobile/`;
- создать `server/`;
- создать `deploy/`;
- добавить `.env.example`;
- описать команды запуска.

## Итерация 1 — backend skeleton

Результат:

- backend запускается локально;
- Docker image собирается;
- Docker Compose поднимает backend, PostgreSQL, Redis, MinIO.

Задачи:

- создать Spring Boot проект;
- добавить healthcheck;
- добавить Flyway/Liquibase migrations;
- подключить PostgreSQL;
- подключить Redis;
- подключить S3 client для MinIO;
- добавить базовые тесты.

## Итерация 2 — auth и пользователи

Результат:

- заранее созданный пользователь может войти;
- клиент получает access/refresh token;
- refresh/logout работают.

Задачи:

- таблицы `users`, `user_credentials`, `sessions`;
- Argon2id password hashing;
- seed пользователей из dev config;
- REST endpoints auth;
- rate limit login;
- тесты auth.

## Итерация 3 — Flutter skeleton и login

Результат:

- APK собирается;
- приложение открывает экран входа;
- пользователь логинится в backend.

Задачи:

- создать Flutter проект в `mobile/`;
- настроить Android package;
- добавить routing/theme;
- добавить HTTP client;
- добавить secure token storage;
- реализовать login/logout.

## Итерация 4 — чаты и сообщения без E2EE

Результат:

- можно видеть список чатов;
- можно отправить тестовое сообщение;
- можно получить историю.

Задачи:

- backend: таблицы chats/messages;
- backend: REST API чатов и сообщений;
- frontend: список чатов;
- frontend: экран диалога;
- frontend: локальная БД;
- временно хранить plaintext только в dev.

Риск: это не production-ready, потому что E2EE ещё не включён.

## Итерация 5 — WebSocket realtime

Результат:

- сообщение появляется у второго клиента без ручного обновления.

Задачи:

- backend WebSocket endpoint;
- auth WebSocket;
- события `message.created`;
- delivery/read receipts;
- reconnect на Flutter;
- offline queue на клиенте.

## Итерация 6 — E2EE для сообщений

Результат:

- backend хранит только ciphertext;
- сообщение расшифровывается только на устройствах участников.

Задачи:

- выбрать крипто-библиотеку;
- реализовать device identity;
- реализовать key bundle API;
- реализовать установку secure session;
- шифровать исходящие сообщения;
- расшифровывать входящие сообщения;
- мигрировать dev plaintext flow на encrypted-only.

Gate: после этой итерации нельзя оставлять plaintext message API.

## Итерация 7 — фото и видео

Результат:

- можно отправлять фото и видео;
- файлы лежат в MinIO в зашифрованном виде.

Задачи:

- backend upload intent;
- pre-signed upload/download URLs;
- ограничения размера файлов;
- Flutter выбор файлов;
- локальное шифрование файла;
- upload progress;
- download/decrypt/cache;
- preview в чате.

## Итерация 8 — 1:1 звонки

Результат:

- два пользователя могут созвониться.

Задачи:

- COTURN в Docker Compose;
- permissions microphone/camera;
- Flutter WebRTC peer connection;
- backend call signaling;
- incoming call screen;
- active call screen;
- call timeout;
- корректное завершение звонка.

## Итерация 9 — групповые чаты

Результат:

- можно создать групповой чат;
- можно отправлять сообщения нескольким участникам.

Задачи:

- backend group chat API;
- роли owner/member;
- добавление/удаление участников;
- frontend экран создания группы;
- E2EE key distribution для группы.

## Итерация 10 — групповые звонки

Результат:

- небольшой групповой звонок работает для ограниченного числа участников.

Задачи:

- mesh WebRTC для группы;
- UI сетки участников;
- participant join/leave events;
- ограничение количества участников;
- тестирование нагрузки на Android-устройствах.

Если качество будет недостаточным, следующая итерация — внедрение SFU.

## Итерация 11 — production hardening

Результат:

- проект можно безопасно поднять на сервере.

Задачи:

- reverse proxy с TLS;
- production env;
- backup PostgreSQL и MinIO;
- structured logging;
- audit logs;
- security checklist;
- smoke tests;
- инструкции деплоя.

## Приоритет MVP

Минимально полезная версия:

1. Login.
2. Список чатов.
3. Текстовые сообщения.
4. WebSocket realtime.
5. E2EE сообщений.
6. Фото.
7. 1:1 звонок.

Групповые звонки лучше делать после стабильной 1:1 связи, потому что WebRTC-группы резко повышают сложность отладки.
