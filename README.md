# SnapIter Backend

SnapIter Backend is a Spring Boot–based service that powers the SnapIter application.  
It provides APIs for SnapIter, with database migrations handled by Flyway.  

---

## 🚀 Getting Started

### Prerequisites
- [Docker](https://www.docker.com/) (for local DB & services)
- [Java 21+](https://adoptium.net/) (or your chosen JDK)
- [Gradle](https://gradle.org/) (wrapper included: `./gradlew`)

---

## 🐳 Run with Docker Compose

Start required services (e.g. Postgres):

```bash
docker-compose up
