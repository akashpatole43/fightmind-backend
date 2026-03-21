# ☕ FightMind AI — Java Backend (`fightmind-backend`)

> **REST API + Auth + DB Orchestration** for the FightMind chatbot  
> **Stack**: Java 17 · Spring Boot 3 · PostgreSQL · Flyway · JWT · Cloudinary  
> **Cost**: ₹0 / $0 per month

---

## 📋 Table of Contents
- [Overview](#overview)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Environment Variables](#environment-variables)
- [Running the Service](#running-the-service)
- [API Endpoints](#api-endpoints)
- [Logging](#logging)
- [Testing](#testing)
- [Docker](#docker)

---

## Overview

This service is the backbone of FightMind. It handles:
- **Authentication** — register, login, JWT token issuance
- **Chat orchestration** — receives user messages, calls the Python AI service, streams responses via SSE
- **Image upload** — uploads to Cloudinary, passes URL to Python pipeline
- **Database** — persists users, chat history, skill profiles via PostgreSQL + Flyway migrations
- **Live events proxy** — forwards event queries to the Python service

### Service Communication
```
React Frontend (localhost:5173)
    │   JWT + REST
    ▼
Java Backend (localhost:8080)        ← THIS SERVICE
    │   Internal HTTP
    ▼
Python AI Service (localhost:8000)
    │
    ├── Gemini API
    ├── TheSportsDB API
    └── PostgreSQL (localhost:5432)
```

---

## Project Structure

```
fightmind-backend/
├── .env.example
├── Dockerfile
├── pom.xml
├── .github/workflows/
│   ├── ci.yml                    ← mvn test on every push
│   └── cd.yml                    ← auto-deploy to Render on main merge
└── src/
    ├── main/java/com/fightmind/
    │   ├── auth/                 ← register, login, JWT filter
    │   ├── chat/                 ← chat endpoint, SSE streaming, Cloudinary
    │   ├── events/               ← live events proxy
    │   ├── user/                 ← user profile, skill profile
    │   ├── model/                ← JPA entities (User, ChatMessage, etc.)
    │   └── config/               ← Security, CORS, App config
    ├── main/resources/
    │   ├── application.yml       ← app config
    │   └── db/migration/         ← Flyway SQL migrations
    └── test/                     ← JUnit 5 + MockMvc tests
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven (included via `mvnw` wrapper)
- Docker Desktop (for PostgreSQL)

### Setup

```bash
# 1. Clone and enter the repo
git clone https://github.com/YOUR_USERNAME/fightmind-backend.git
cd fightmind-backend

# 2. Configure environment
copy .env.example .env          # Windows
# cp .env.example .env          # Mac/Linux
# Edit .env and fill in your values

# 3. Start PostgreSQL (via docker-compose at project root)
cd ..
docker-compose up postgres -d

# 4. Run the service
cd fightmind-backend
./mvnw spring-boot:run          # Mac/Linux
mvnw.cmd spring-boot:run        # Windows
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_URL` | ✅ Yes | `jdbc:postgresql://localhost:5432/fightmind` |
| `DB_USERNAME` | ✅ Yes | PostgreSQL username |
| `DB_PASSWORD` | ✅ Yes | PostgreSQL password |
| `JWT_SECRET` | ✅ Yes | Secret key for JWT signing (min 32 chars) |
| `JWT_EXPIRY_MS` | Optional | Token expiry in ms (default: 86400000 = 24h) |
| `CLOUDINARY_URL` | ✅ Yes | Full Cloudinary URL (from dashboard) |
| `PYTHON_SERVICE_URL` | ✅ Yes | `http://localhost:8000` |

---

## Running the Service

### Development
```bash
./mvnw spring-boot:run
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

---

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | ❌ | Register new user |
| `POST` | `/api/auth/login` | ❌ | Login, returns JWT |
| `POST` | `/api/chat/send` | ✅ JWT | Send message (text + optional image), SSE stream |
| `GET` | `/api/chat/history` | ✅ JWT | Retrieve chat history |
| `GET` | `/api/user/profile` | ✅ JWT | Get user profile + skill level |
| `GET` | `/api/events/today` | ✅ JWT | Today's live events |
| `GET` | `/actuator/health` | ❌ | Health probe (Docker, UptimeRobot) |

---

## Logging

Logging is configured via `src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.fightmind.auth:      DEBUG     # change per package
    com.fightmind.chat:      DEBUG
    com.fightmind.pipeline:  INFO
    org.hibernate.SQL:       DEBUG     # show SQL queries (dev only)
```

### Viewing Logs

#### 🖥️ Local Development
```bash
# Logs stream directly to console when running
./mvnw spring-boot:run

# Filter to errors only (Windows PowerShell)
./mvnw spring-boot:run 2>&1 | Select-String "ERROR"
```

#### 🐳 Docker
```bash
docker-compose logs -f fightmind-backend
docker-compose logs -f fightmind-backend | grep ERROR
```

#### ☁️ Production (Render.com)
Render Dashboard → your service → **Logs** tab

---

## Testing

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=AuthControllerTest

# Run with coverage report
./mvnw test jacoco:report
# Open target/site/jacoco/index.html
```

---

## Docker

```bash
# From project root — starts backend + postgres + model
docker-compose up fightmind-backend postgres fightmind-model
```

> ⚠️ The backend requires PostgreSQL and the Python service to be healthy before it starts (enforced by `depends_on` conditions in `docker-compose.yml`).
