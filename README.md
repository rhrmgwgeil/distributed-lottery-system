# High-Concurrency Distributed Lottery System

A robust, distributed lottery and roulette backend service designed for enterprise e-commerce marketing campaigns. Built with Java 21 and Spring Boot 3, this system is architected to handle massive traffic spikes without compromising data integrity.

It implements a two-stage probability algorithm, utilizing Redis for atomic inventory pre-deduction and anti-bot rate limiting, coupled with RabbitMQ for asynchronous database persistence and Dead Letter Queue (DLQ) compensation.

## 🚀 Features & Technical Description

### Core Features

* **Two-Stage Draw Algorithm**: Separates probability calculation (in-memory) from inventory deduction, preventing unnecessary database or cache hits.

* **Zero Over-Selling (Race Condition Prevention)**: Uses Redis `DECR` atomic operations as the absolute source of truth during traffic peaks.

* **Dynamic Configuration (LiveOps)**: Supports real-time updates for prize inventory, probabilities, and user draw limits. Utilizes a **Cache-Aside / Write-Through** strategy to sync PostgreSQL updates instantly to Redis.

* **Asynchronous Persistence**: Adopts an `HTTP 202 Accepted` polling mechanism. Draw results are sent to RabbitMQ and written to PostgreSQL smoothly by consumers, achieving traffic shaping (Peak Shaving).

* **Distributed Transaction Consistency**: Implements the **Transactional Outbox Pattern** via Redis Native Transactions (`MULTI/EXEC`) and a Scheduler to guarantee that no winning messages are lost even if the application crashes.

* **Defense in Depth**: Features JWT-based authentication, Redis-backed JWT blocklist for secure logouts, and user-level rate limiting interceptors to block malicious bot traffic.

### Tech Stack

* **Language**: Java 21

* **Framework**: Spring Boot 3.3.x, Spring Security, Spring Data JPA

* **Database**: PostgreSQL 16 (or higher)

* **Cache & Rate Limiting**: Redis (Spring Data Redis)

* **Message Broker**: RabbitMQ (Spring AMQP)

* **Documentation**: SpringDoc OpenAPI (Swagger 3.0)

* **Testing**: JUnit 5, Mockito

## 🔐 Default Account Info

When the application starts for the first time, a `DataSeeder` will automatically create a default administrator account.

* **Username**: `admin`

* **Password**: `admin`

* **Scope/Role**: `ADMIN`

> **⚠️ Security Note**: For security reasons, the system requires this default administrator to change their password upon the first login. Any administrative API calls (like creating users or activities) will be blocked and throw an exception until the `is_password_changed` flag becomes `true`.

## 📊 Sequence Diagram

*Please refer to the sequence diagram below for the core lottery draw execution flow (Redis Pre-deduction -> Transactional Outbox -> RabbitMQ -> DB Persistence).*

<!-- TODO: Insert Sequence Diagram here -->

## 🗄️ Database Schema Description

All entities inherit from a `BaseEntity` which automatically manages JPA Auditing fields: `created_by`, `created_at`, `updated_by`, and `updated_at`.

### 1. `users`

Stores system users and administrators.

| **Column** | **Type** | **Constraints** | **Description** | 
| ----- | ----- | ----- | ----- | 
| `id` | BIGINT | PK, Auto Increment | User ID | 
| `username` | VARCHAR | Unique, Not Null | Login username | 
| `password` | VARCHAR | Not Null | Bcrypt hashed password | 
| `scope` | VARCHAR | Not Null | Role/Permissions (e.g., ADMIN, USER) | 
| `is_password_changed` | BOOLEAN | Default `false` | Flag to force password reset on first login | 

### 2. `activities`

Stores the configuration for different lottery campaigns.

| **Column** | **Type** | **Constraints** | **Description** | 
| ----- | ----- | ----- | ----- | 
| `id` | BIGINT | PK, Auto Increment | Activity ID | 
| `name` | VARCHAR | Not Null | Campaign Name | 
| `status` | INT | Not Null | 0: Draft, 1: Active, 2: Offline, 3: Paused | 
| `max_draws_per_user` | INT | Not Null, Min 1 | Maximum allowed draws per user | 
| `start_time` | TIMESTAMP | Not Null | Campaign start time | 
| `end_time` | TIMESTAMP | Not Null | Campaign end time | 

### 3. `prizes`

Stores prize configurations linked to a specific activity.

| **Column** | **Type** | **Constraints** | **Description** | 
| ----- | ----- | ----- | ----- | 
| `id` | BIGINT | PK, Auto Increment | Prize ID | 
| `activity_id` | BIGINT | FK | Associated activity | 
| `name` | VARCHAR | Not Null | Prize name | 
| `stock` | INT | Not Null | Physical inventory | 
| `probability` | INT | Not Null | Win probability (1-100%) | 
| `prize_type` | INT | Not Null | 1: Physical, 2: Virtual, 3: No Prize | 
| `version` | BIGINT | `@Version` | Optimistic lock to prevent concurrent DB updates | 

### 4. `draw_tickets`

Acts as the proof of a draw attempt. Used for polling (HTTP 202) and idempotent MQ consumption.

| **Column** | **Type** | **Constraints** | **Description** | 
| ----- | ----- | ----- | ----- | 
| `ticket_id` | VARCHAR | PK, UUID | Unique tracking ID returned to frontend | 
| `activity_id` | BIGINT | Not Null | Activity ID | 
| `user_id` | BIGINT | Not Null | User who initiated the draw | 
| `prize_id` | BIGINT | Nullable | ID of the won prize (Null if pending/failed) | 
| `status` | INT | Not Null | 0: INIT (Processing), 1: SUCCESS, 2: FAILED | 

### 5. `user_activity_counters`

Records the final state of how many times a user has participated in a specific activity.

| **Column** | **Type** | **Constraints** | **Description** | 
| ----- | ----- | ----- | ----- | 
| `id` | BIGINT | PK, Auto Increment | Counter ID | 
| `user_id` | BIGINT | Not Null | User ID | 
| `activity_id` | BIGINT | Not Null | Activity ID | 
| `current_draw_count` | INT | Default 0 | Ground truth of total draws by the user | 
| `version` | BIGINT | `@Version` | Optimistic locking for accurate DB increments |