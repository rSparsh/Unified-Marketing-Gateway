🚀 Unified Marketing Gateway

A reactive, extensible backend platform for multi-channel ad broadcasting.

📌 Overview

Unified Marketing Gateway is a backend system that enables businesses to broadcast marketing messages across multiple communication platforms (Telegram, WhatsApp, SMS, Email, etc.) through a single unified API.

The system is designed with reactive, scalable, rate-limited, and fault-tolerant pipelines, allowing safe and controlled fan-out of outbound marketing requests.

The project is currently in progress.
Phase 1 implements the Telegram broadcast flow using Spring WebFlux.

🎯 Current Features (Phase 1 — Telegram Integration)
✔ Reactive Pipeline for Outbound Requests

Built using Spring WebFlux with fully non-blocking flows.

✔ Async, High-Throughput Execution

Uses Flux pipelines to process multiple requests concurrently without thread starvation.

✔ Rate-Limited Execution

Custom reactive rate-limiter ensures controlled outbound API calls, preventing provider throttling.

✔ Automatic Retries with Exponential Backoff

Handles transient failures (timeouts, 5xx responses) with a reactive retry strategy and backoff logic.

✔ Structured Flow

Request execution pipeline includes:

Validation layer

Payload builder

Rate-limiter

Async WebClient executor

Retry & fallback handling

✔ Clean, Extensible Design

The system is structured to easily add integrations for:

WhatsApp

SMS gateways

Email services

Push notification platforms

🛠 Tech Stack
Component	Technology
Language	Java 17
Framework	Spring Boot (WebFlux)
Concurrency	Project Reactor (Flux/Mono)
HTTP Client	WebClient
Build Tool	Maven/Gradle
Deployment	(Planned) Docker + Kubernetes
Future Plans	Redis rate-limiter, Kafka event ingestion
📐 High-Level Architecture
+------------------------------+
|  Unified Marketing Gateway   |
+------------------------------+
|
v
+-----------------------+
|  Platform Selector    |
+-----------------------+
|       |      |
v       v      v
Telegram   SMS   WhatsApp
|
v
+---------------------------+
| Reactive Broadcast Flow   |
+---------------------------+
| Validate Request
| Build Payload
| Apply Rate Limit
| Execute Async Call
| Retry (Exponential Backoff)
v
Provider API

🚧 Roadmap
🔹 Phase 2 (Upcoming)

WhatsApp integration

SMS provider integration

Unified error/response model

Platform-level rate limits

🔹 Phase 3

Kafka ingestion for bulk event-driven campaigns

Dashboard APIs

Per-tenant rate limits

Dynamic workflow configuration

🔹 Phase 4

End-to-end multi-channel campaign automation

Analytics & delivery metrics

Rule-engine for intelligent routing

🤝 Contributions

Since this is a personal development project, contributions are welcome via pull requests or suggestions through issues.

📄 License

MIT License (or specify if different)

📬 Contact

Author: Sparsh Raj
GitHub: https://github.com/rSparsh

Email: sparshraj6a@gmail.com