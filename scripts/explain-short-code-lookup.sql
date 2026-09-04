-- Run after docker compose up -d postgres and after Flyway has created url_mapping.
-- Replace AbC123x with a short code that exists in your local database.
explain analyze
select id, short_code, original_url, created_at, expires_at, click_count
from url_mapping
where short_code = 'AbC123x';

-- Optional comparison for interview evidence:
-- 1. create a copy without the short_code index
-- 2. load the same rows into it
-- 3. run the same EXPLAIN ANALYZE query and compare planning/execution time
