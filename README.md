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

## 📖 API Documentation (Swagger UI)

This project integrates **SpringDoc OpenAPI (Swagger 3.0)** to provide interactive REST API documentation and an execution playground.

* **Swagger UI Dashboard**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI Specification (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

### Endpoint Catalog & Access Control

The backend APIs are categorized into 5 functional areas. Access is secured using Bearer JWT authentication:

| Controller | Endpoint Path | HTTP Method | Required Role | Description & Side Effects |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | `/api/v1/auth/login` | `POST` | Public | Authenticates credentials and returns a JWT token. |
| | `/api/v1/auth/logout` | `POST` | All Users | Invalidates the active JWT by placing it in the Redis blocklist. |
| **User Management** | `/api/v1/users/change-password` | `POST` | All Users | Updates the password. **Mandatory** for the default `admin` on first login. |
| | `/api/v1/users` | `POST` | `ADMIN` | Creates a new user profile. |
| | `/api/v1/users` | `GET` | `ADMIN` | Lists all users. |
| | `/api/v1/users/{id}` | `GET` / `PUT` / `DELETE` | `ADMIN` | Retrieves, updates, or deletes a specific user profile. |
| **Activity Admin** | `/api/v1/activities` | `POST` | `ADMIN` | Creates a new lottery activity campaign configuration. |
| | `/api/v1/activities/{id}` | `PUT` | `ADMIN` | Updates an existing lottery activity campaign. |
| **Prize Admin** | `/api/v1/prizes` | `POST` | `ADMIN` | Creates a new prize. For physical prizes, initializes inventory in Redis. |
| | `/api/v1/prizes/{id}` | `PUT` | `ADMIN` | Updates prize details and synchronizes updated inventory to Redis. |
| **Draw/Lottery Engine** | `/api/v1/draws` | `POST` | All Users | Initiates a draw request (supports batch draws: count 1 to 50). Returns `202 Accepted`. |
| | `/api/v1/draws/{ticketId}/result` | `GET` | All Users | Polls the final outcome (`INIT`, `SUCCESS`, or `FAILED`) of a draw ticket. |

---

### Step-by-Step API Integration & Testing Flow

Below is the recommended workflow to initialize the database configurations and perform draw simulations directly via Swagger UI:

#### 1. Obtain the Default JWT Token
1. Open [Swagger UI](http://localhost:8080/swagger-ui/index.html).
2. Go to **Authentication Controller** -> `POST /api/v1/auth/login`.
3. Click **Try it out** and execute with the default credentials:
   ```json
   {
     "username": "admin",
     "password": "admin"
   }
   ```
4. Copy the `token` string from the JSON response body.

#### 2. Authorize Swagger UI
1. Click the green **Authorize** button (lock icon) at the top-right of the page.
2. Paste the copied token value into the input field and click **Authorize**. Close the dialog.
3. Subsequent requests from Swagger UI will automatically include the `Authorization: Bearer <token>` header.

#### 3. Change Default Password (Required for Admin)
> [!IMPORTANT]
> To enforce system security, all Admin operations (creating activities, prizes, and users) are rejected with a validation exception until the default password has been changed.
1. Go to **User Controller** -> `POST /api/v1/users/change-password`.
2. Click **Try it out** and input:
   ```json
   {
     "oldPassword": "admin",
     "newPassword": "yourNewAdminPassword123"
   }
   ```
3. Execute. After receiving a success response, your old token will become invalid.
4. **Re-login** at `POST /api/v1/auth/login` using username `admin` and password `yourNewAdminPassword123`. Copy the new JWT token and apply it via the **Authorize** button.

#### 4. Configure a New Lottery Campaign
1. Go to **Activity Controller** -> `POST /api/v1/activities`.
2. Configure a new activity (e.g. `Mid-Autumn Festival Lucky Draw`):
   ```json
   {
     "name": "Mid-Autumn Festival Lucky Draw",
     "status": "ACTIVE",
     "maxDrawsPerUser": 10,
     "startTime": "2026-06-20T00:00:00+08:00",
     "endTime": "2026-06-30T23:59:59+08:00"
   }
   ```
3. Execute and note the returned `id` (usually `1` for the first campaign).

#### 5. Configure Prizes for the Activity
1. Go to **Prize Controller** -> `POST /api/v1/prizes`.
2. Add a physical prize (e.g., iPhone 15 Pro, with stock = `5`, probability = `10%`):
   ```json
   {
     "activityId": 1,
     "name": "iPhone 15 Pro",
     "stock": 5,
     "probability": 10,
     "prizeType": 1
   }
   ```
3. Execute. The system will create the prize and sync its stock (`5`) into Redis atomically.
4. (Optional) Add a dummy "No Prize / Thank You" option with type `3` to cover the rest of the probability:
   ```json
   {
     "activityId": 1,
     "name": "Thank you for participating!",
     "stock": 99999,
     "probability": 90,
     "prizeType": 3
   }
   ```

#### 6. Perform a Batch Draw
1. Go to **Draw Controller** -> `POST /api/v1/draws`.
2. Request a batch of draws (e.g., drawing `3` tickets at once):
   ```json
   {
     "activityId": 1,
     "count": 3
   }
   ```
3. Execute. The server does not block on database operations; it validates rules, checks Redis stock, pushes events to RabbitMQ, and returns a `202 Accepted` response with 3 tickets in `INIT` status.

#### 7. Retrieve Draw Results (Polling)
1. Copy one of the `ticketId` UUIDs from the draw response.
2. Go to **Draw Controller** -> `GET /api/v1/draws/{ticketId}/result`.
3. Execute. Once RabbitMQ consumes and persists the event, the status will show either `SUCCESS` (specifying the winning `prizeId`) or `FAILED` (if the draw lost or stock ran out).


## 📊 Sequence Diagram

*Please refer to the sequence diagram below for the core lottery draw execution flow (Redis Pre-deduction -> Transactional Outbox -> RabbitMQ -> DB Persistence).*

![Draw Happy Path Sequence Diagram](https://github.com/rhrmgwgeil/distributed-lottery-system/blob/main/sequenceDiagram/Draw_Happy_Path.png?raw=true)

![Cunsumer Happy Path Sequence Diagram](https://raw.githubusercontent.com/rhrmgwgeil/distributed-lottery-system/refs/heads/main/sequenceDiagram/Cunsumer_Happy_Path.png)

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