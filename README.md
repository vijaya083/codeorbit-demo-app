# CodeOrbit Demo App

CodeOrbit Demo App is a small subscription SaaS backend for demonstrating account registration, plan subscriptions, billing, local payment recording, refunds, and simulated notifications. It is a demo and evaluation application for CodeOrbit; it is not the CodeOrbit product itself.

## Technology

- Java 21
- Spring Boot 3.5.16
- Spring Web, Spring Data JPA, and Bean Validation
- H2 in-memory database
- Maven

## Architecture

The application is a modular monolith organized by domain feature. Authentication delegates account creation and uses a separate token service. Subscription workflows use user and plan services, create invoices through billing services, and notify through a small notification boundary. Payments update invoices, and refunds validate against successful payments and previously completed refunds. Controllers use request and response DTOs instead of exposing persistence entities.

Passwords are stored as BCrypt hashes. The generated login token is a lightweight opaque demo token: there is no token persistence, request authentication filter, or production-grade session management. This authentication mechanism is intended only for this demo/evaluation application and is not production security. Email delivery is simulated in application logs; payments and refunds are local records and do not contact a payment provider.

## Run locally

Requirements: JDK 21 and Maven 3.9 or newer.

```sh
mvn spring-boot:run
```

The application starts on `http://localhost:8080` without external services. It uses an H2 in-memory database at `jdbc:h2:mem:codeorbit`; data is reset when the application stops. Hibernate creates/updates the schema on startup. BASIC, PRO, and BUSINESS plans are seeded if they are not already present.

The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:codeorbit`, user `sa`, blank password).

## Tests

```sh
mvn test
```

## API

JSON request and response bodies are used. Validation and domain errors return a consistent JSON error object with timestamp, HTTP status, message, and request path.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register an account (`email`, `password`, `firstName`, `lastName`) |
| `POST` | `/api/auth/login` | Verify credentials and return a demo token |
| `GET` | `/api/plans` | List active subscription plans |
| `POST` | `/api/subscriptions` | Subscribe a user (`userId`, `planId`) and create an initial invoice |
| `POST` | `/api/subscriptions/{id}/cancel` | Cancel an active subscription |
| `GET` | `/api/users/{userId}/subscriptions` | List a user's subscriptions |
| `GET` | `/api/users/{userId}/invoices` | List a user's invoices |
| `POST` | `/api/invoices/{invoiceId}/payments` | Record payment for an open invoice (`amount`) |
| `POST` | `/api/payments/{paymentId}/refunds` | Refund a successful payment (`amount`) |

A user may have one active subscription at a time. A payment must match its invoice amount and can only be recorded for an open invoice. A refund must be positive and cannot exceed the payment's remaining refundable balance.
