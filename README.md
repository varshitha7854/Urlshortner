# URL Shortener

A Spring Boot URL shortener with PostgreSQL persistence, Redis lazy-loading cache, validated APIs, async click tracking, and local Docker Compose infrastructure.

## Architecture

```mermaid
flowchart LR
    Client[Client] --> App[Spring Boot API]
    App -->|POST /api/shorten| Postgres[(PostgreSQL)]
    App -->|GET /{shortCode}| Redis[(Redis cache)]
    Redis -->|miss| Postgres
    App -->|async increment| Postgres
    App -->|302 redirect| Client
```

## Design Choices

Short codes use random 7-character Base62 strings with a DB collision check and a unique constraint as the final guard. This keeps URLs short, for example `http://localhost:8080/AbC123x`, while avoiding predictable sequential IDs. The tradeoff is collision handling, handled here with retries and the `uk_url_mapping_short_code` database constraint.

The original URL is stored unchanged in PostgreSQL and retrieved by `short_code`. A URL shortener still needs requests to hit this application's domain unless the original domain is configured to route traffic to this service.

Redirect lookups use a lazy-loading cache: Redis is checked first, Postgres is queried on a miss, then Redis is populated. A write-through strategy could also cache during `POST /api/shorten`, but lazy loading keeps creation simple and caches only links that are actually used. Redis TTL is capped at 24 hours and shortened to the link's `expires_at`, so expired links naturally leave the cache. Deleted links should call `UrlCacheService.evict(shortCode)` in the delete workflow.

Click tracking is asynchronous through `@Async`, so the hot redirect path does not wait for the analytics update.

## Schema And Indexing

Flyway creates `url_mapping` with:

- `id`
- `short_code`
- `original_url`
- `created_at`
- `expires_at`
- `click_count`

The short-code lookup is backed by a real PostgreSQL index and uniqueness constraint:

```sql
create index idx_url_mapping_short_code on url_mapping (short_code);
constraint uk_url_mapping_short_code unique (short_code)
```

The JPA entity also declares `@Table(indexes = @Index(name = "idx_url_mapping_short_code", columnList = "short_code"))` so the indexed query intent is visible in code.

## Run Locally

Prerequisites:

- JDK 17+; JDK 21 is recommended
- Docker Desktop

Start Postgres and Redis:

```bash
docker compose up -d postgres redis
```

Run the app:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the full stack in Docker:

```bash
docker compose --profile app up --build
```

## APIs

Create a short URL:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d "{\"originalUrl\":\"https://example.com/articles/spring-cache\"}"
```

Create with a custom expiry:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d "{\"originalUrl\":\"https://example.com\",\"expiresAt\":\"2026-12-31T23:59:59Z\"}"
```

Redirect:

```bash
curl -i http://localhost:8080/{shortCode}
```

Analytics:

```bash
curl http://localhost:8080/api/analytics/{shortCode}
```

Invalid URLs are rejected unless they are absolute `http` or `https` URLs with a host.

## Query Plan Verification

After creating at least one short URL, run:

```bash
docker compose exec -T postgres psql -U urlshortener -d urlshortener < scripts/explain-short-code-lookup.sql
```

On Windows PowerShell:

```powershell
Get-Content .\scripts\explain-short-code-lookup.sql | docker compose exec -T postgres psql -U urlshortener -d urlshortener
```

The expected plan for `where short_code = ...` should use `idx_url_mapping_short_code` or the unique constraint index.

## Benchmark Methodology

Use k6 to compare DB-only redirects against Redis-backed redirects.

1. Start Postgres and Redis with `docker compose up -d postgres redis`.
2. Start the app with cache disabled: `APP_CACHE_ENABLED=false ./mvnw spring-boot:run`.
3. Create one short URL and export its code as `SHORT_CODE`.
4. Run `k6 run -e SHORT_CODE=$SHORT_CODE -e VUS=25 -e DURATION=60s benchmarks/redirect-cache.k6.js`.
5. Restart the app with cache enabled: `APP_CACHE_ENABLED=true ./mvnw spring-boot:run`.
6. Warm the cache once with `curl -i http://localhost:8080/$SHORT_CODE`.
7. Run the same k6 command again.
8. Record p95 latency, request rate, machine specs, and Docker resource limits.

Results table:

| Mode | VUs | Duration | p95 latency | Notes |
| --- | ---: | ---: | ---: | --- |
| DB-only | 25 | 60s | To be measured locally | `APP_CACHE_ENABLED=false` |
| Redis cache | 25 | 60s | To be measured locally | warm cache, `APP_CACHE_ENABLED=true` |

Do not claim a fixed percentage improvement until these numbers are measured on the machine used for the interview/demo. The formula is `(dbOnlyP95 - cachedP95) / dbOnlyP95 * 100`.

## Scaling Notes

The application is stateless. To scale horizontally, run multiple app instances behind a load balancer with shared Redis and PostgreSQL. Redis handles the hot redirect cache, PostgreSQL remains the source of truth, and async analytics updates keep redirects low-latency.
