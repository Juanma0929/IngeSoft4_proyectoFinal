# Spec: Monolithic Average Speed

## Scope

Version 1 calculates SITM-MIO average speed by active route and month in a single JVM and a single
local process.

## Inputs

- Active routes CSV: `lines-241-ActiveGT.csv`.
- Datagrams CSV: MiniPilot or Pilot datagrams.
- Output path under `results/`.

## Required Behavior

- Read all active routes from `LINEID`.
- Read datagrams using configured columns or configured headerless indexes.
- Keep only datagrams whose route is active.
- Parse coordinates using `--coordinate-scale`.
- Sort points by route, bus, and timestamp.
- Create segments from consecutive points for the same route and bus.
- Reject invalid rows and invalid segments deterministically.
- Aggregate by route and month.
- Write deterministic CSV output sorted by route and month.

## Out Of Scope

- No concurrency.
- No distributed components.
- No database.
- No runtime network dependency.

