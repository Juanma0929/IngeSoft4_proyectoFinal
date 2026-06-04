# Plan: Monolithic Average Speed

## Approach

1. Parse CLI options.
2. Load active routes.
3. Read and clean datagrams.
4. Sort cleaned points by route, bus, and timestamp.
5. Calculate valid speed segments.
6. Aggregate by route and month.
7. Fill active route/month combinations without data with zero values.
8. Write the output CSV.

## Data Decisions

See `docs/calculation-assumptions.md`.

## Verification

- Unit tests for CSV parsing, timestamp parsing through datagram reading, distance calculation,
  segment filtering, aggregation, output writing, and CLI parsing.
- Integration-style v1 test for a full small flow.
- Manual local run against `docs/datagrams-MiniPilot.csv`.

