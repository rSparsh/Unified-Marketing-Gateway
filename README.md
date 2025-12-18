<h1 align="center">🚀 Unified Marketing Gateway</h1>

A **reactive, state-driven backend platform** for reliable multi-channel message delivery with idempotency, delivery tracking, reconciliation, and fallback routing.

---

## 📌 Overview

**Unified Marketing Gateway** is a backend system that enables broadcasting marketing or notification messages across multiple communication channels—**Telegram, WhatsApp, and SMS (Twilio)**—through a single unified API.

The system is designed to handle **high fan-out workloads** safely using:
- reactive, non-blocking execution
- strict idempotency guarantees
- delivery state tracking
- webhook-driven updates
- reconciliation for eventual consistency
- state-driven fallback routing

Rather than being a simple message sender, the platform behaves like a **reliable notification delivery system** that remains correct under retries, concurrency, webhook duplication, and partial failures.

---

## 🎯 Key Features

### ✔ Unified Multi-Channel API
Send notifications to multiple recipients across:
- **Telegram**
- **WhatsApp (Cloud API)**
- **SMS (Twilio)**

All channels are triggered via a single request model and processor pipeline.

---

### ✔ Reactive, Non-Blocking Execution
- Built using **Spring WebFlux**
- Uses **Flux / Mono** pipelines for async fan-out
- Supports controlled concurrency without thread starvation

---

### ✔ Strong Idempotency Guarantees
- Database-backed idempotency using a composite key:
- Prevents duplicate sends across:
- retries
- concurrent requests
- fallback routing
- webhook replays

---

### ✔ Delivery State Machine
Each message follows a tracked lifecycle:
- Channel-aware state transitions
- Telegram / SMS stop at SENT
- WhatsApp supports full lifecycle via webhooks

---

### ✔ Webhook Processing (WhatsApp)
- Idempotent webhook handling
- Safe against duplicate and out-of-order events
- Monotonic state transitions enforced using precedence rules
- Provider message ID (`wamid`) used as the external correlation key

---

### ✔ Reconciliation for Eventual Consistency
- Scheduled reconciliation job detects messages stuck in:
    - QUEUED
    - SENT
    - DELIVERED
- Helps identify:
    - missing webhooks
    - provider delays
    - execution gaps
- Enables safe recovery and observability without blind retries

---

### ✔ State-Driven Fallback Routing
- Automatic fallback from **WhatsApp → SMS**
- Triggered only when delivery state indicates failure or prolonged staleness
- Guaranteed at-most-once fallback per recipient
- Fully idempotent and auditable

---

### ✔ Metrics & Observability
- Prometheus-compatible metrics via **Micrometer**
- Tracks:
    - send attempts
    - successes / failures
    - in-flight requests
    - latency
    - reconciliation events
    - fallback executions

---

### ✔ Persistent Storage (H2 / JPA)
- JPA-backed persistence for:
    - idempotency records
    - delivery state
    - SMS message tracking
- H2 used for simplicity and local development
- Easily portable to Postgres / MySQL

---

## 🛠 Tech Stack

| Component | Technology |
|---------|-----------|
| Language | Java 17 |
| Framework | Spring Boot (WebFlux) |
| Reactive | Project Reactor (Flux / Mono) |
| HTTP Client | WebClient |
| Persistence | Spring Data JPA + H2 |
| Messaging APIs | Telegram Bot API, WhatsApp Cloud API, Twilio SMS |
| Metrics | Micrometer + Prometheus |
| Build Tool | Maven |

---

## 🧱 System Design

[//]: # (![Unified Marketing Gateway – System Design]&#40;docs/system-design.png&#41;)
<img src="docs/system-design.png" alt="Alt text" width="700" height="400">

---

## 🚀 Getting Started
[Refer to the Getting Started Wiki](docs/GettingStarted.md)

---

## 🤝 Contributions

This is a personal engineering project focused on system design and reliability.
Suggestions, discussions, and pull requests are welcome.

---

## 📬 Contact

Author: Sparsh Raj

GitHub: https://github.com/rSparsh

Email: sparshraj6a@gmail.com
