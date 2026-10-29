# Payment Processing & Transaction Management System

A backend application built with Java and Spring Boot to demonstrate payment creation, transaction status management, secure authentication, idempotency, asynchronous processing, and caching.

## Tech Stack

- **Language:** Java
- **Backend:** Spring Boot, Spring Security
- **Database:** PostgreSQL
- **Database Migrations:** Flyway
- **Messaging:** Apache Kafka
- **Caching:** Redis
- **Authentication:** JWT
- **Build Tool:** Maven
- **Testing:** JUnit, Mockito
- **API Testing:** Postman
- **Infrastructure:** Docker

## Features

- User registration and login
- JWT-based authentication
- Role-based access control
- RESTful payment APIs
- Idempotent payment creation using an idempotency key
- Asynchronous payment processing with Kafka
- Payment status tracking: `PENDING`, `PROCESSING`, `SUCCESS`, and `FAILED`
- Redis caching for payment lookups
- Cache invalidation when payment status changes
- React frontend for registration, login, payment creation, and authenticated payment history
- PostgreSQL persistence and Flyway database migrations
- Request validation and exception handling

## Architecture

```text
Client / Postman
       |
       v
Spring Boot REST API
       |
       +------> PostgreSQL
       |        Persistent payment data
       |
       +------> Redis
       |        Cached payment responses
       |
       +------> Apache Kafka
                Asynchronous payment events
                       |
                       v
                Payment Consumer
                       |
                       v
                Payment Processor
                       |
                       v
                Update payment status
```

## Prerequisites

Install the following before running the application:

- Java version compatible with the project
- Docker Desktop
- PostgreSQL, or a Docker-based PostgreSQL setup
- Maven, if you are not using the included Maven Wrapper
- Postman (optional, for API testing)

## Configuration

The application uses `src/main/resources/application.properties`.

Configure the following environment variables in your run configuration or shell:

| Variable | Description |
|---|---|
| `DB_PASSWORD` | Password for the PostgreSQL user |
| `JWT_SECRET` | Secret used to sign JWTs |

Do not commit real passwords, JWT secrets, or other credentials to GitHub.

## Running the Application

### 1. Clone the repository

```bash
git clone https://github.com/uNexusOp/payment-processing-system.git
cd payment-processing-system
```

### 2. Start Kafka and Redis

```bash
docker compose up -d
```

Check that the required containers are running:

```bash
docker ps
```

### 3. Configure PostgreSQL

Create a PostgreSQL database named `payment_processing` and configure its connection details in `application.properties`.

Set `DB_PASSWORD` and `JWT_SECRET` before starting the application.

Flyway runs the database migrations when Spring Boot starts.

### 4. Run Spring Boot

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```bat
mvnw.cmd spring-boot:run
```

The application is configured to use port `8080` by default.

## API Testing

Use Postman to test the application.

Typical workflow:

1. Register a user.
2. Log in and obtain a JWT.
3. Send the JWT as a Bearer token to protected endpoints.
4. Create a payment with an `Idempotency-Key` header.
5. Repeat the same request with the same key to test idempotency.
6. Retrieve payment history or a payment by ID.
7. Observe Kafka-driven payment status transitions.

Base URL:

```text
http://localhost:8080
```

The React frontend is in `frontend/` and can be started with `npm start` from that directory. It uses the authenticated payment API to show the current user's payment history.

Payment endpoints:

- `POST /api/payments` — create a payment (requires an `Idempotency-Key` header)
- `GET /api/payments` — list the authenticated user's payments
- `GET /api/payments/{id}` — retrieve one of the authenticated user's payments

## Running Tests

Run the automated test suite:

```bash
./mvnw test
```

On Windows:

```bat
mvnw.cmd test
```

## Project Status

The backend and React frontend are implemented, including authenticated payment history. Dockerizing the full stack and deployment remain future work.

## Author

**Ankit Yadav**

GitHub: [uNexusOp](https://github.com/uNexusOp)