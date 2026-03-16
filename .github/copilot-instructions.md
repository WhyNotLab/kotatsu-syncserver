# GitHub Copilot Instructions

## Project Overview

This is **Kotatsu Sync Server** — a self-hostable synchronization backend for the [Kotatsu](https://github.com/KotatsuApp/Kotatsu) Android manga reader app. It stores and syncs user favorites, reading history, and categories across multiple devices.

## Tech Stack

- **Language**: Kotlin (JVM 21+)
- **Framework**: [Ktor](https://ktor.io/) (server-side, Netty engine)
- **Database**: MySQL / MariaDB via [Ktorm](https://www.ktorm.org/) ORM
- **Migrations**: Flyway
- **Auth**: JWT (via `ktor-server-auth-jwt`)
- **Serialization**: `kotlinx.serialization` (JSON)
- **Templating**: Mustache (for email templates)
- **Connection Pool**: HikariCP
- **Password Hashing**: Argon2
- **Build Tool**: Gradle (Kotlin DSL)

## Project Structure

```
src/
  main/
    kotlin/org/kotatsu/
      Application.kt         # Entry point
      plugins/               # Ktor plugin installations (auth, routing, DB, etc.)
      routes/                # Route handlers (auth, favourites, history, deeplinks)
      mail/                  # Mail sender abstraction (console + SMTP)
    resources/
      application.conf       # Ktor configuration
      db/migration/          # Flyway SQL migration scripts
  test/
    kotlin/                  # Unit and integration tests
```

## Coding Conventions

- Follow the **official Kotlin code style** (`kotlin.code.style=official` in `gradle.properties`)
- Use `suspend` functions for all Ktor route handlers and database calls
- Keep route logic thin — delegate to separate handler/service functions when logic grows
- Use Ktorm DSL for database queries; avoid raw SQL except in Flyway migration scripts
- Prefer `kotlinx.serialization` `@Serializable` data classes for request/response models
- Group related routes in files under `routes/` (e.g., `AuthRoutes.kt`, `FavouriteRoutes.kt`)
- Plugins are installed in `plugins/` and called from `Application.kt`

## Environment Variables

Key variables (see `.env.example` for the full list):

| Variable | Description |
|---|---|
| `DATABASE_HOST` | MySQL/MariaDB host |
| `DATABASE_USER` | Database user |
| `DATABASE_PASSWORD` | Database password |
| `DATABASE_NAME` | Database name |
| `DATABASE_PORT` | Database port |
| `JWT_SECRET` | Secret for signing JWT tokens |
| `ALLOW_NEW_REGISTER` | `true`/`false` — enable/disable new user registration |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASSWORD` / `SMTP_FROM` | Optional SMTP settings for email |
| `BASE_URL` | Public server URL (used in email links) |

## Running Locally

```shell
# Start the database (Docker)
docker compose up -d db

# Run the server
./gradlew run
```

## Building

```shell
./gradlew shadowJar
# Output: build/libs/kotatsu-syncserver-0.0.1.jar
```

## Testing

```shell
./gradlew test
```
