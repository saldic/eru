# ERU Backend

ERU is a backend REST API for a learning-focused scrolling platform.

The idea behind the project is simple: instead of endlessly scrolling low-value content, users should be able to browse short educational posts such as facts, quotes, and theories. The backend stores the content, exposes it through an API, and supports authentication, content management, and basic user interaction.

This project was developed as a 3rd semester backend exam project with a layered architecture based on the technologies and patterns used during the semester.

---

## Vision

The purpose of ERU is to explore how a feed-based platform can be used for something more meaningful than entertainment alone.

Rather than building a full-scale social platform, the project focuses on a smaller and more realistic backend scope:
- managing educational content
- handling users and authentication
- supporting simple interaction with content
- exposing the system through a REST API
- keeping the structure clear and easy to explain in an exam setting

The project is intentionally limited in scope. The goal was not to build everything, but to build a backend that is coherent, testable, and aligned with the semester material.

---

## Tech Stack

### Core Technologies
- **Java 17**
- **Maven**
- **Javalin 6.0.2**
- **Hibernate ORM 7.2.3.Final**
- **PostgreSQL** with **PostgreSQL JDBC Driver 42.7.7**
- **JWT** via **java-jwt 4.5.0**
- **jBCrypt 0.4**

### Testing
- **JUnit 5**
- **Testcontainers**

### Runtime / Infrastructure
- **Docker**
- **PostgreSQL Docker image: `postgres:16-alpine`**
- **Application base image: `amazoncorretto:17-alpine`**

### Additional Libraries
- Jackson
- SLF4J / Logback
- Lombok

---

## Architecture

The application is built with a simple layered structure:

- **Routes** define the HTTP endpoints
- **Controllers** handle request and response logic
- **Services** contain validation and business rules
- **DAOs** handle persistence through Hibernate/JPA
- **PostgreSQL** stores the application data

Request flow:

`Client -> Route -> Controller -> Service -> DAO -> Hibernate/JPA -> PostgreSQL`

This structure was chosen because it keeps responsibilities separated, makes the request flow easy to follow, and fits well with the patterns used in class.

---

## Main Features

The implemented backend includes:

- user registration and login with JWT
- endpoint for the currently authenticated user
- CRUD operations for content
- filtering content by type and active status
- role-based route protection
- a `User` ↔ `Role` relationship
- content interactions through `UserInteraction`
- optional AI-based elaboration of content
- service-level unit tests
- integration testing with Testcontainers

Some parts of the codebase are present but not central to the project presentation:
- advanced role administration
- extended interaction features
- AI integration details beyond the core endpoint

---

## Domain Model

### Content
`Content` is the main entity in the system and represents the items shown in the feed.

Typical fields include:
- `id`
- `title`
- `body`
- `contentType`
- `category`
- `source`
- `author`
- `active`
- `createdAt`

### User
`User` is used for authentication and authorization.

Typical fields include:
- `id`
- `username`
- `passwordHash`
- `roles`
- `createdAt`

### Role
`Role` is used together with `User` to control access to protected routes.

### UserInteraction
`UserInteraction` represents a user’s interaction with a piece of content.
This supports the idea of a feed where users can do more than just read — for example react, save, or interact with content in a structured way.

---

## API Overview

### Public Endpoints

- `GET /api/v1/`
- `GET /api/v1/health`
- `GET /api/v1/routes`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/content`
- `GET /api/v1/content/{id}`
- `GET /api/v1/content/{id}/interactions`

### Protected Endpoints

#### User
- `GET /api/v1/auth/me`
- `POST /api/v1/content/{id}/interactions`

#### Admin
- `POST /api/v1/content`
- `PUT /api/v1/content/{id}`
- `DELETE /api/v1/content/{id}`

### Optional Endpoint
- `POST /api/v1/ai/elaborate`

---

## Example Requests

HTTP request examples are available in:

`src/main/resources/http`

They are grouped by feature to make local testing easier.

---

## Local Setup

### 1. Start PostgreSQL

The easiest way to run the database locally is with Docker:

```bash
docker compose up -d
```

This starts PostgreSQL with the default local setup used by the project.

### 2. Configure the application

The application can run with sensible local defaults, but environment variables can also be used.

Supported configuration includes:

- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_URL`
- `JWT_SECRET`
- `OPENAI_API_KEY` *(optional)*
- `OPENAI_MODEL` *(optional)*

If needed, configuration can also be loaded from a local properties file.

### 3. Build and run the application

Compile the project:

```bash
mvn clean compile
```

Package the runnable JAR:

```bash
mvn clean package
```

Then run `app.Main` from your IDE, or run the packaged JAR directly:

```bash
java -jar target/app.jar
```

The API starts at:

`http://localhost:7070/api/v1`

---

## Testing

Run tests with:

```bash
mvn test
```

The test setup includes:
- unit tests for service logic
- integration testing with Testcontainers
- route-level testing for authentication and content flows

If Docker is not available, Testcontainers-based tests may be skipped depending on the setup.

---

## Deployment

The project includes files for container-based deployment:

- `Dockerfile`
- `docker-compose.yml`
- GitHub Actions workflow configuration

The intended deployment flow is:

1. build and test the project with Maven
2. package the application
3. build a Docker image
4. push the image to a container registry
5. run the image on a server with environment-based configuration

---

## Semester Relevance

This project connects directly to the main backend topics from the semester:

- REST API design with Javalin
- DTO-based request/response handling
- JPA and Hibernate for persistence
- entity relationships
- DAO and service layers
- password hashing and JWT authentication
- PostgreSQL setup with Docker
- testing with JUnit and Testcontainers

The project is intentionally designed to be easy to explain in an exam setting, with a clear request flow and limited but relevant functionality.

---

## Project Scope

ERU is not meant to be a full social platform.
It is a focused backend project built to demonstrate solid understanding of backend architecture, persistence, security, testing, and API design.

The scope is deliberately practical:
- small enough to finish
- large enough to demonstrate key backend concepts
- structured in a way that is easy to maintain and present

---

## Authors

Developed as part of the 3rd semester backend exam project.

Add contributor names here if you want the README to reflect your group members.

---

## Repository Structure

```text
src/
 ├─ main/
 │   ├─ java/
 │   └─ resources/
 └─ test/
```

The project follows a standard Maven structure and keeps application code, configuration, and tests separated.
