# Samuel Lair's Submission For Trase Backend Take-home Test

## How To Run The API

These instructions are written assuming you are running the API on a POSIX compliant operating system such as Linux,
macOS, or Unix. If you are using a non-POSIX operating system, you may need to adjust the shell commands.

From the project root, start the database(s) and API:

```
docker compose up --build
```

The API will be available at:

```
http://localhost:8080
```

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Swagger UI is disabled when `SPRING_PROFILES_ACTIVE=prod`.

Demo mode: if you don't want to manually manage tokens, open Swagger UI and authorize with the
seeded `admin` / `admin123!` user.
Swagger UI also provides a Quick login dropdown in dev/test (top middle-left in the Swagger UI bar): select a user and click Authorize to prefill the token.

Actuator endpoints (also visible in Swagger UI):

```
http://localhost:8080/actuator/health
http://localhost:8080/actuator/health/liveness
http://localhost:8080/actuator/health/readiness
http://localhost:8080/actuator/info
http://localhost:8080/healthz
```

### Authentication (JWT)

All API endpoints require a JWT. Obtain a token via:

```
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123!"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` in requests, or paste it into Swagger UI's
Authorize modal.

JWT keys:
- `security.jwt.private-key` and `security.jwt.public-key` accept `classpath:` or `file:` paths.
- In dev/test, local RSA keys are provided in `src/main/resources/keys`.
- In prod, set `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` (path to PEM files) and `SPRING_PROFILES_ACTIVE=prod`.

Default profile is `dev` (set `SPRING_PROFILES_ACTIVE` explicitly for production).

Dev/test users (seeded automatically in `dev` and `int-test` profiles; not present in prod):
- `admin` / `admin123!` (ADMIN)
- `ops` / `ops123!` (OPERATOR)
- `runner` / `runner123!` (RUNNER)
- `reader` / `reader123!` (READER)

## How To Run Tests

The unit and integration tests are separated into `unitTest` and `intTest` Gradle tasks. The standard `test` task runs
both. Docker is required because Testcontainers starts Postgres automatically for integration/perf tests.

Run the unit tests:

```
./gradlew unitTest
```

Run the integration tests (the task drops and recreates the integration test schema, so dropAll must be allowed):

```
./gradlew intTest
```

Integration tests use Testcontainers and require Docker to be running.

Run the full test suite:

```
./gradlew test
```

Run the optional Swagger UI browser tests (Playwright; Docker required; first run may download browsers):

```
./gradlew uiTest
```

## Smoke Test

Run a quick end-to-end smoke test against a running API (Docker Compose or local run):

```
./scripts/smoke-test.sh
```

Override the base URL if needed:

```
BASE_URL=http://localhost:8080 ./scripts/smoke-test.sh
```

### Coverage reporting

The combined JaCoCo report is generated after the full test suite and can also be run explicitly:

```
./gradlew coverage
```

HTML report output:

```
build/reports/jacoco/jacocoCombinedReport/html/index.html
```

Coverage thresholds (combined unit + integration) are enforced at 90% minimum for line, branch, method, and instruction
coverage.

### Performance profiling (optional)

Run the optional perf profiling test suite (Docker is required because Testcontainers starts Postgres automatically):

```
./gradlew perfProfile
```

You can control dataset sizes and paging via JVM properties:

```
./gradlew perfProfile \
  -Dperf.agents=5000 \
  -Dperf.tasks=5000 \
  -Dperf.taskAgents=8 \
  -Dperf.runs=20000 \
  -Dperf.pageSize=50 \
  -Dperf.page=10 \
  -Dperf.deepPage=100 \
  -Dperf.afterId=5000
```

Supported flags: `perf.agents`, `perf.tasks`, `perf.taskAgents`, `perf.runs`, `perf.pageSize`, `perf.page`,
`perf.deepPage`, `perf.afterId`.

The raw `EXPLAIN (ANALYZE, BUFFERS)` output is written to:

```
build/perf/perf-profile.txt
```

PerfProfile uses SQL that mirrors the repository query shapes; Hibernate-generated SQL may differ slightly depending on
runtime settings.

The Gradle HTML test report with console output is available at:

```
build/reports/tests/perfProfile/index.html
```

## HTML Documentation

Generate Javadoc HTML:

```
./gradlew javadoc
```

Output is written to:

```
build/docs/javadoc/index.html
```

## API Documentation

### Auth

- `POST /auth/login`
- `POST /auth/logout`

Request body:

```
{
  "username": "admin",
  "password": "admin123!"
}
```

Response body:

```
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresAt": "2026-01-31T12:34:56Z",
  "userId": 1,
  "roles": ["ADMIN"]
}
```

Logout (revokes the current token):

```
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer <token>"
```

### Authorization roles

- **ADMIN**: full access
- **OPERATOR**: manage agents/tasks and task runs
- **RUNNER**: start/update task runs
- **READER**: read-only access

### Agents

- `GET /agents`
- `POST /agents`
- `GET /agents/{id}`
- `PUT /agents/{id}`
- `DELETE /agents/{id}`

Request body for `POST /agents` and `PUT /agents/{id}`:

```
{
  "name": "Agent A",
  "description": "Handles tasks"
}
```

Response body for `GET /agents/{id}`:

```
{
  "id": 1,
  "name": "Agent A",
  "description": "Handles tasks"
}
```

### Tasks

- `GET /tasks`
- `GET /task` (alias of `/tasks`)
- `GET /task/{id}` (alias of `/tasks/{id}`)
- `POST /tasks`
- `GET /tasks/{id}`
- `PUT /tasks/{id}`
- `DELETE /tasks/{id}`

Request body for `POST /tasks` and `PUT /tasks/{id}`:

```
{
  "title": "Task 1",
  "description": "Do important work",
  "supportedAgentIds": [1, 2]
}
```

You can also supply a single agent using either `supportedAgentId` or `supported_agent_id`:

```
{
  "title": "Task 1",
  "description": "Do important work",
  "supported_agent_id": 1
}
```

Response body for `GET /tasks/{id}`:

```
{
  "id": 1,
  "title": "Task 1",
  "description": "Do important work",
  "supportedAgentIds": [1, 2],
  "supportedAgentId": null
}
```

When exactly one agent is supported, `supportedAgentId` is populated with that id.

If any `supportedAgentIds` do not exist, the API returns `400`.

List endpoints (`GET /agents`, `GET /tasks`, `GET /task-runs`) accept optional pagination and sorting query
parameters: `page`, `size`, `sort`, and `afterId` (for example, `?page=0&size=25&sort=id,desc`).
Defaults: `page=0`, `size=50`, `sort=id,asc`.

Example requests:

```
curl "http://localhost:8080/agents?size=10&sort=id,asc"
curl "http://localhost:8080/tasks?page=1&size=5&sort=id,desc"
```

Note: Paging combined with an `@EntityGraph` on a to-many relationship can result in larger intermediate result sets
and/or multiple queries; the work scales with the number of relationships materialized (~K * a_page).

### Soft deletion behavior

Delete operations are soft deletes. Deleted agents/tasks are excluded from list and get endpoints.

### Response fields

Responses intentionally omit `createdAt`/`updatedAt` even though those timestamps are stored in the database.

### Audit logging

Write operations are audited in `agents_audit`, `tasks_audit`, and `task_runs_audit`, keyed by the authenticated user
and request ID.
Read-only audit endpoints (admin-only):
- `GET /audits/agents`
- `GET /audits/tasks`
- `GET /audits/task-runs`

### Token revocation

Logged-out tokens are recorded in the `revoked_tokens` table and rejected immediately on subsequent requests.

Expired revoked tokens are cleaned up on a schedule (default: hourly). Configure with
`security.jwt.revocation-cleanup-cron`.

### Rate limiting

Rate limiting is enabled via Resilience4j. Defaults are `120` requests per `1m`, `timeout=0`. When the limit is hit,
responses include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` (seconds until reset).
Override via:

```
resilience4j.ratelimiter.instances.api.limit-for-period=120
resilience4j.ratelimiter.instances.api.limit-refresh-period=1m
resilience4j.ratelimiter.instances.api.timeout-duration=0
```

### Task Runs (Bonus)

- `POST /task-runs` starts a task with a specific agent.
- `GET /task-runs` lists task runs. Use `?status=RUNNING` to list active runs.
- `PATCH /task-runs/{id}` updates the status of a task run.
- `POST /task-runs` accepts an optional `Idempotency-Key` header to dedupe retries.

Request body for `POST /task-runs`:

```
{
  "taskId": 1,
  "agentId": 2
}
```

Response body:

```
{
  "id": 10,
  "taskId": 1,
  "agentId": 2,
  "status": "RUNNING",
  "startedAt": "2026-01-30T19:01:00Z",
  "completedAt": null
}
```

Request body for `PATCH /task-runs/{id}`:

```
{
  "status": "COMPLETED"
}
```

### Audits (Admin only)

- `GET /audits/agents`
- `GET /audits/tasks`
- `GET /audits/task-runs`

### Error Format

Errors are returned as JSON with the following shape:

```
{
  "timestamp": "2026-01-30T19:01:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/tasks",
  "validationErrors": {
    "title": "title is required"
  }
}
```

## Rate Limiting

All endpoints are rate limited. When the limit is exceeded, the API returns HTTP 429 with the standard error format.
Each response includes an `X-Request-Id` header to help correlate logs, plus `X-RateLimit-Limit`,
`X-RateLimit-Remaining`, and `X-RateLimit-Reset`.

## Assumptions

- The original prompt lists `GET /task` once; this API implements `GET /tasks` for consistency.
- Tasks can be supported by multiple agents (bonus requirement). Requests accept `supportedAgentIds` as well as a single
  `supportedAgentId` / `supported_agent_id` field.
- Agents and tasks are soft deleted by setting `deleted_at`, and deleted records are excluded from reads. Repeated
  deletes are idempotent and return `204`.
- Agent names are unique among active (non-deleted) rows.
- The integration test database is separate from the primary database and is reset before integration tests run.

## Complexity Analysis

This section summarizes how each endpoint scales based on the **current** Spring MVC controllers, Spring Data JPA repositories, and Liquibase migrations in this repo.

### Variables

- **A_total**: total rows in `agents`
- **A**: “active” agents (`deleted_at IS NULL`)
- **T_total**: total rows in `tasks`
- **T**: “active” tasks (`deleted_at IS NULL`)
- **E**: rows in `task_supported_agents` (task↔agent relationships)
- **a_page**: average supported agents per *task in the returned page* (≈ relationships returned / tasks returned)
- **R**: total rows in `task_runs`
- **R_s**: task_runs with a given status
- **K**: page size (default 50 via `@PageableDefault(size = 50, sort = "id")`)
- **O**: page offset (roughly `pageNumber * K`)
- **N**: count of agent ids supplied in a request body (`supportedAgentIds`, etc.)

> Note on pagination: these endpoints support **offset/limit** paging (via `page`/`size`) and an optional **keyset** mode via `afterId`. List endpoints use `Slice`, so offset mode avoids count queries, but large **O** (deep pages) still makes queries progressively more expensive because the DB must skip `O` rows before returning `K`. Keyset mode avoids the deep offset cost entirely.
These list endpoints use `Slice<T>`, so Spring Data does not issue a count query for offset pagination.

### Indexes (from migrations)

No index assumptions are made unless the migration file and index name are cited.

- Primary keys:
  - `2026_01_30-01-create_agents_table.sql`: `agents(id)`
  - `2026_01_30-02-create_tasks_table.sql`: `tasks(id)`
  - `2026_01_30-04-create_task_runs_table.sql`: `task_runs(id)`
- Join table primary key: `2026_01_30-03-create_task_supported_agents_table.sql`: `task_supported_agents(task_id, agent_id)`.
- Secondary indexes:
  - `2026_01_30-04-create_task_runs_table.sql`: `idx_task_runs_status` on `task_runs(status)`.
  - `2026_01_30-03-create_task_supported_agents_table.sql`: `idx_task_supported_agents_agent_id` on `task_supported_agents(agent_id)`.
  - `2026_01_30-07-add_soft_delete_indexes.sql`: `idx_agents_deleted_at` on `agents(deleted_at)`.
  - `2026_01_30-07-add_soft_delete_indexes.sql`: `idx_tasks_deleted_at` on `tasks(deleted_at)`.
  - `2026_01_31-08-add_active_id_indexes.sql`: `idx_agents_active_id` on `agents(id) WHERE deleted_at IS NULL`.
  - `2026_01_31-08-add_active_id_indexes.sql`: `idx_tasks_active_id` on `tasks(id) WHERE deleted_at IS NULL`.
  - `2026_01_31-11-add_task_runs_status_id_index.sql`: `idx_task_runs_status_id` on `task_runs(status, id)`.
  - `2026_01_31-09-create_task_run_idempotency_table.sql`: `task_run_idempotency_key_uq` on `task_run_idempotency(idempotency_key)`.
  - `2026_01_31-09-create_task_run_idempotency_table.sql`: `task_run_idempotency_task_run_id_idx` on `task_run_idempotency(task_run_id)`.
  - `2026_01_31-10-add_agent_name_unique_index.sql`: `idx_agents_name_active_unique` on `agents(name) WHERE deleted_at IS NULL`.

### Important ORM notes

- `GET /tasks` uses a **two-query strategy**: first page tasks without fetching collections, then load supported agents for the page’s task IDs.
  - The second query fetches the `supportedAgents` collection for just those `K` tasks, so work scales with relationships materialized (≈ `K * a_page`) without paging a collection fetch.
  - This avoids the JPA pitfall where paginating a collection fetch-join/entity-graph can produce duplicate roots or in-memory pagination.
- `Slice<T>` queries (used by list endpoints here) avoid the **count query**; they only fetch page content. If a
  `Page<T>` is used elsewhere, expect an additional count query.
- The `deleted_at` indexes help with `WHERE deleted_at IS NULL`, but because results are also **sorted by `id`**, the DB may still choose plans that scan/filter when many rows are active. A common optimization for “soft delete + list by id” is a composite or partial index such as `(deleted_at, id)` or `id WHERE deleted_at IS NULL`.

### Endpoint complexity

Below, “DB time” is described in terms of rows scanned/processed/returned, which is more actionable than pure Big-O for SQL.

#### **GET /agents**
Controller → `AgentService.listAgents(pageable, afterId)` → `AgentDao.findAllByDeletedAtIsNull(Pageable)` (offset) or
`AgentDao.findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(afterId, Pageable)` (keyset)

- **DB time:**
  - Offset mode (`afterId` absent): fetch `K` agents ordered by `id` with `OFFSET O` ⇒ work tends to grow with `(O + K)` rows touched (no count query).
  - Keyset mode (`afterId` present): no count query; fetch `K` agents with `id > afterId`.
- **App time:** map `K` entities → DTOs ⇒ `O(K)`.
- **Space:** `O(K)`.

**Worst case input:** large `O` (deep offset page) with very large `A` (e.g., millions of active agents).

#### **POST /agents**
Controller → `AgentService.createAgent(...)` → `AgentDao.existsByNameAndDeletedAtIsNull(name)` → `AgentDao.save(...)`

- **DB time:** 1 indexed existence check + 1 insert.
- **App time / space:** constant.

**Worst case input:** none meaningful (single-row insert).

#### **GET /agents/{id}**
Controller → `AgentService.getAgent(id)` → `AgentDao.findByIdAndDeletedAtIsNull(id)`

- **DB time:** PK lookup by `id` (plus `deleted_at` filter) ⇒ ~`O(log A_total)`, returns ≤ 1 row.
- **App time / space:** constant.

**Worst case input:** `id` not found (still an indexed lookup).

#### **PUT /agents/{id}**
Controller → `AgentService.updateAgent(id, ...)` → `AgentDao.findByIdAndDeletedAtIsNull(id)` →
`AgentDao.existsByNameAndDeletedAtIsNullAndIdNot(name, id)` (+ dirty checking / update)

- **DB time:** PK lookup + indexed existence check + update 1 row.
- **App time / space:** constant.

**Worst case input:** `id` not found (indexed lookup + exception).

#### **DELETE /agents/{id}**
Controller → `AgentService.deleteAgent(id)` → `AgentDao.findById(id)` → `AgentDao.save(...)` (only if not already deleted)

- **DB time:** PK lookup + update 1 row (sets `deleted_at`), unless already deleted (then no update).
- **App time / space:** constant.

**Worst case input:** active agent exists (does the update). If the row exists and is already soft-deleted, the service returns early and the controller still returns 204; if the id doesn’t exist, the endpoint returns 404.

---

#### **GET /tasks**
Controller → `TaskService.listTasks(pageable, afterId)` → `TaskDao.findAllByDeletedAtIsNull(Pageable)` (offset) or
`TaskDao.findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(afterId, Pageable)` (keyset) +
`TaskDao.findAllByIdInAndDeletedAtIsNull(ids)`

- **DB time:**
  - Offset mode (`afterId` absent): fetch `K` tasks ordered by `id` with `OFFSET O`, then load supported agents.
    - Page query: skips `O`, returns `K` tasks ⇒ work tends to grow with `(O + K)` tasks touched.
  - Keyset mode (`afterId` present): no count query; fetch `K` tasks with `id > afterId`, then load supported agents.
  - Relationship load: for the returned tasks, touches up to ~`K * a_page` relationship rows (and agent rows).
- **App time:** map `K` tasks and enumerate supported agents ⇒ `O(K + K * a_page)`.
- **Space:** hydrated entities + response ⇒ `O(K + K * a_page)`.

**Worst case input:** deep offset page (`O` large) where tasks on that page have large supported-agent sets (large `a_page`).

#### **POST /tasks**
Controller → `TaskService.createTask(...)` → `resolveAgents(...)` → `AgentDao.findAllByIdInAndDeletedAtIsNull(ids)` → `TaskDao.save(...)`

- **DB time:** resolve `N` agent ids + insert 1 task + insert `N` join rows.
  - Agent resolution: typically uses PK lookups for `N` ids (often via bitmap plan) ⇒ scales with `N`.
- **App time:** validate `N` ids, map entities ⇒ `O(N)`.
- **Space:** hold `N` agents/ids ⇒ `O(N)`.

**Worst case input:** very large `supportedAgentIds` list (`N` large), especially with missing ids (still resolves and then throws).

#### **GET /tasks/{id}**
Controller → `TaskService.getTask(id)` → `TaskDao.findByIdAndDeletedAtIsNull(id)` (**entity graph**)

- **DB time:** PK lookup for task + load its supported agents ⇒ ~`O(log T_total + a_task)` where `a_task` is the task’s supported-agent count.
- **App time / space:** `O(1 + a_task)`.

**Worst case input:** a task that supports a very large number of agents.

#### **PUT /tasks/{id}**
Controller → `TaskService.updateTask(id, ...)` →  
`TaskDao.findByIdAndDeletedAtIsNull(id)` (**entity graph**) + `resolveAgents(ids)` → `AgentDao.findAllByIdInAndDeletedAtIsNull(ids)` (+ update join rows)

- **DB time:** PK lookup for task + load existing supported agents + resolve `N` agent ids + update join table rows (delete/insert as needed).
- **App time:** set operations + mapping ⇒ `O(a_task + N)`.
- **Space:** `O(a_task + N)` (existing + new supported agent collections).

**Worst case input:** updating a task with large existing support set and a large incoming `supportedAgentIds`.

#### **DELETE /tasks/{id}**
Controller → `TaskService.deleteTask(id)` → `TaskDao.findById(id)` → `TaskDao.save(...)` (only if not already deleted)

- **DB time:** PK lookup + update 1 row (sets `deleted_at`); **idempotent** in service (returns if already deleted).
- **App time / space:** constant.

**Worst case input:** task exists and not yet deleted (does the update); already-deleted tasks return 204 after the lookup.

---

#### **POST /task-runs**
Controller → `TaskRunService.startTaskRun(request, idempotencyKey)` →  
optional `TaskRunIdempotencyDao.findByIdempotencyKey(...)` +  
`TaskDao.findByIdAndDeletedAtIsNullBasic(taskId)` + `AgentDao.findByIdAndDeletedAtIsNull(agentId)` +
`TaskDao.isAgentSupported(taskId, agentId)` + `TaskRunDao.save(...)` + optional `TaskRunIdempotencyDao.save(...)`

- **DB time:** optional idempotency lookup by key + two PK lookups + **existence check** in `task_supported_agents`
  (index-backed) + insert 1 run (+ insert 1 idempotency row when the header is present).
- **App time / space:** constant (no in-memory scan of supported agents).

**Worst case input:** invalid task/agent ids (still two PK lookups + one existence check).

#### **GET /task-runs**
Controller → `TaskRunService.listTaskRuns(status, pageable, afterId)` →  
offset mode: `TaskRunDao.findAllBy(Pageable)` or `TaskRunDao.findByStatus(status, Pageable)`  
keyset mode: `TaskRunDao.findAllByIdGreaterThanOrderByIdAsc(...)` or
`TaskRunDao.findByStatusAndIdGreaterThanOrderByIdAsc(...)`

- **DB time:**
  - Offset mode (`afterId` absent):
    - With `status`: fetch page using `idx_task_runs_status` (`2026_01_30-04-create_task_runs_table.sql`), skips `O`, returns `K` ⇒ scales with `(O + K)` over the matching status set.
      - Keyset mode benefits from `idx_task_runs_status_id` (`2026_01_31-11-add_task_runs_status_id_index.sql`).
    - Without `status`: fetch page over all runs ⇒ scales with `(O + K)` over `R`.
  - Keyset mode (`afterId` present): no count query; fetch `K` runs with `id > afterId` (and optional status filter).
- **App time:** map `K` runs ⇒ `O(K)`.
- **Space:** `O(K)`.

**Worst case input:** deep offset page (`O` large), or a status with very high cardinality (`R_s` large).

#### **PATCH /task-runs/{id}**
Controller → `TaskRunService.updateTaskRunStatus(id, status)` → `TaskRunDao.findById(id)` → `TaskRunDao.save(...)`

- **DB time:** PK lookup + update 1 row.
- **App time / space:** constant.

**Worst case input:** id not found (still one lookup).

---

#### **GET /audits/agents**, **/audits/tasks**, **/audits/task-runs**
Controller → `AuditQueryService.list*Audits(Pageable)` → `*AuditDao.findAll(Pageable)`

- **DB time:** count + fetch page ordered by `id` ⇒ scales with `(O + K)` over the audit table.
- **App time:** map `K` audit records ⇒ `O(K)`.
- **Space:** `O(K)`.

**Worst case input:** deep page (`O` large) with a large audit table.

### Biggest scaling cliff

Even with pagination, **GET /tasks** is the endpoint most likely to hurt first if tasks are “dense” (many agents per task), because the relationship load scales with the number of relationships returned (≈ `K * a_page`).

### Perf findings vs claims (current run)

Using `perfProfile` defaults (~2,000 agents, ~2,000 tasks, ~5 agents/task, ~5,000 task runs, `pageSize=50`, `page=0`,
`deepPage=100`, `afterId=5000`) on **2026-01-31**:

- **Offset vs keyset:** deep offset queries still sort/scan many rows, while keyset queries use
  `idx_agents_active_id` (`2026_01_31-08-add_active_id_indexes.sql`) / `idx_tasks_active_id` (`2026_01_31-08-add_active_id_indexes.sql`)
  with `id > afterId` and avoid the deep offset cost.
- **Task reads now split:** the task page query stays lean (no join), and the second query loads supported agents for
  the page’s ids, matching the `K * a_page` relationship scaling note without pagination artifacts.
- **Status filtering:** the planner chose `idx_task_runs_status_id` (`2026_01_31-11-add_task_runs_status_id_index.sql`) for both status-only
  and `status + id` queries in this run (the `idx_task_runs_status` index from `2026_01_30-04-create_task_runs_table.sql` still exists but wasn’t selected here).

**Excerpt (from `build/perf/perf-profile.txt`):**

```
2026-01-31T19:52:37.761954Z SQL: SELECT id, name, description, created_at, updated_at FROM agents WHERE deleted_at IS NULL ORDER BY id LIMIT 50 OFFSET 5000
2026-01-31T19:52:37.762257Z   ->  Sort  (cost=10.36..10.37 rows=2 width=474) (actual time=0.430..0.491 rows=2000.00 loops=1)
2026-01-31T19:52:37.762732Z         ->  Bitmap Heap Scan on agents  (cost=4.28..10.35 rows=2 width=474) (actual time=0.044..0.184 rows=2000.00 loops=1)
2026-01-31T19:52:37.770753Z SQL: SELECT t.id, t.title, t.description, t.created_at, t.updated_at, tsa.agent_id FROM tasks t LEFT JOIN task_supported_agents tsa ON tsa.task_id = t.id WHERE t.deleted_at IS NULL AND t.id IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50)
2026-01-31T19:52:37.771778Z   ->  Bitmap Heap Scan on task_supported_agents tsa  (cost=4.68..61.56 rows=51 width=16) (actual time=0.001..0.001 rows=5.00 loops=50)
2026-01-31T19:52:37.778333Z SQL: SELECT id, task_id, agent_id, status, started_at, completed_at FROM task_runs WHERE status = 'RUNNING' ORDER BY id LIMIT 50 OFFSET 0
2026-01-31T19:52:37.779205Z               ->  Bitmap Index Scan on idx_task_runs_status_id  (cost=0.00..4.37 rows=12 width=0) (actual time=0.046..0.046 rows=2500.00 loops=1)
```
