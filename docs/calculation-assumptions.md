# Calculation Assumptions

This document records the decisions used by the speed calculators. The project statement asks for
average speed by active route and month, but it does not define every cleaning rule. These rules make
the result reproducible.

## Inputs

- Active routes come from `lines-241-ActiveGT.csv`, column `LINEID`.
- Datagram files are treated as headerless when using the assignment files.
- Relevant datagram indexes are:
  - `4`: latitude, scaled by `10_000_000`.
  - `5`: longitude, scaled by `10_000_000`.
  - `7`: lineId / route id.
  - `10`: datagram event timestamp.
  - `11`: bus id.

## Route Filtering

The program keeps every route that appears in the active routes file, including route `-1`
(`TESTGT1`) because it is present in the provided active route list. If the evaluator decides that
test routes must be excluded, that should be handled as an explicit data rule, not as an implicit
code assumption.

## Timestamp Formats

Supported deterministic timestamp formats include:

- `2019-05-27 20:14:43`
- `2019-05-27T20:14:43Z`
- `30-MAY-19 11.59.59.000000 PM`

Local timestamps are interpreted in UTC only to keep month grouping deterministic across machines.

## Segment Rules

For each route and bus, points are sorted by timestamp. A speed segment is created only between
consecutive points for the same route and bus.

A segment is rejected when:

- the time delta is zero or negative;
- the time delta is greater than `--max-gap-minutes` (`10` by default);
- the computed speed is not finite;
- the computed speed is greater than `--max-speed-kmh` (`120` by default).

Distance is calculated with the Haversine formula. The primary average speed is:

```text
avg_speed_kmh = total_distance_km / total_time_hours
```

The output also includes `avg_segment_speed_kmh`, which is the simple arithmetic mean of segment
speeds. It is useful for diagnostics, but `avg_speed_kmh` is the value that answers the assignment.

