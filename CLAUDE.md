# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SnapIter backend is a Spring Boot application written in Kotlin that uses:
- **Spring Boot 4.0.0-SNAPSHOT** with reactive programming (WebFlux)
- **Spring Data R2DBC** for reactive database access
- **PostgreSQL** database with R2DBC driver
- **Flyway** for database migrations
- **Kotlin coroutines** and reactor for async programming
- **JUnit 5** for testing with Spring Boot Test

## Common Commands

### Building and Running
```bash
# Build the project
./gradlew build

# Run the application (development mode with auto-reload)
./gradlew bootRun

# Build executable JAR
./gradlew bootJar

# Clean build directory
./gradlew clean
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests "com.snapiter.backend.BackendApplicationTests"
```

### Development
```bash
# List all available tasks
./gradlew tasks

# Build and run continuously (file watching)
./gradlew bootRun --continuous
```

## Architecture

**Technology Stack:**
- **Language**: Kotlin with JVM target (Java 21)
- **Framework**: Spring Boot 4.0 with reactive stack
- **Database**: PostgreSQL with R2DBC (reactive database connectivity)
- **Migration Tool**: Flyway (located in `src/main/resources/db/migration/`)
- **Build Tool**: Gradle with Kotlin DSL

**Project Structure:**
- Main application class: `src/main/kotlin/com/snapiter/backend/BackendApplication.kt`
- Resources: `src/main/resources/` (contains `application.properties` and database migrations)
- Tests: `src/test/kotlin/com/snapiter/backend/`

**Key Configuration:**
- Application properties: `src/main/resources/application.properties`
- Database migrations: `src/main/resources/db/migration/`
- Spring Boot DevTools enabled for development hot reload

**Reactive Architecture:**
The application uses Spring WebFlux with R2DBC for fully reactive, non-blocking data access. Use reactive types (`Mono`, `Flux`) and Kotlin coroutines for asynchronous operations.