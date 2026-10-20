Real-Time Pricing Engine

A high-performance, low-latency market data processing engine developed with Spring Boot 3 and Java 21. The service ingests live financial ticks via WebSocket, computes rolling 24-hour metrics in-memory, maintains thread-safe state persistence across restarts, and broadcasts optimized delta payloads to downstream clients.

Architecture Overview

Upstream Ingestion (FeedListener.java)

Connects to the LiveFXHub WebSocket feed (wss://v3-prod.livefxhub.com/ws/algo). Performs authentication and currency pair subscriptions. Includes automated reconnection with backoff on connection drops.

Metrics Computation (PricingService.java)

Processes raw incoming ticks to calculate rolling 24-hour high, low, and percentage change values locally in memory using thread-safe data structures (ConcurrentHashMap).

State Persistence (state.json)

Persists calculated state to disk periodically and during graceful shutdown. Automatically restores previous state on service restart.

Downstream Broadcasting (FrontendSocketHandler.java)

Distributes live market updates to connected clients at /ws/market with minified JSON payloads to keep bandwidth well below 10KB.

Containerization (Dockerfile & docker-compose.yml)

Provides a multi-stage Docker build and compose configuration with persistent volume mapping for containerized deployments.

Tech Stack

Language: Java 21

Framework: Spring Boot 3

Build Tool: Apache Maven

Protocol: WebSockets (Spring WebSocket)

Deployment: Docker, Docker Compose

Getting Started

Build and Compile

./mvnw clean test-compile


Run Service

./mvnw spring-boot:run


The application runs on port 8080.

Web Dashboard

Access the frontend monitoring interface at:

http://localhost:8080/index.html

Run with Docker

docker compose up --build


Upstream Integration Status

During live connection verification with wss://v3-prod.livefxhub.com/ws/algo, the upstream endpoint responds with:

{"event": "error", "message": "invalid credentials"}


(WebSocket Closure Code: 4003)

The engine catches this error gracefully and maintains an active reconnection loop. Once updated credentials are supplied, feed ingestion and broadcasting proceed automatically.
