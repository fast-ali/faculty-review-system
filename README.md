# Faculty Review System (FRS)

This repository contains a Spring Boot web application for a Faculty Review System. The project was previously named `BMS`; the UI and documentation were updated to reflect the new focus.

## Requirements
- Java 17+
- Docker (for Postgres)

## Quick start (Docker + IntelliJ friendly)
1) Copy the environment template:
   - `cp .env.example .env`
2) Start Postgres:
   - `docker compose up -d`
3) Run the app:
   - IntelliJ: run `BmsApplication`
   - CLI: `./mvnw spring-boot:run`

## One-command dev startup
- `./scripts/dev-up.sh`

The login page template is at `src/main/resources/templates/login.html` and static assets (CSS, JS, images) live under `src/main/resources/static/`.

## Database migrations
- Schema changes are managed with Flyway scripts in `src/main/resources/db/migration`.
- Current baseline migration: `V1__create_app_users_table.sql`.
- JPA is configured with `spring.jpa.hibernate.ddl-auto=validate` to prevent silent schema drift.

## Authentication foundation
- User records live in `app_users`.
- Username and email are unique.
- Account flags: `enabled`, `account_locked`.

## Error handling
- UI-facing signup errors are centrally handled in `GlobalExceptionHandler`.
- Duplicate username/email and password policy errors are mapped to user-friendly messages for Thymeleaf templates.
