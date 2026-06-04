# Architecture Drivers And Quality Attributes

## Functional Driver: Correctness

The system must calculate average SITM-MIO bus speed by route and month for every active route in
the pilot route file.

Correctness has highest priority because performance comparisons are meaningless if the three
versions do not produce the same result.

Validation criteria:

- active routes are loaded from `lines-241-ActiveGT.csv`;
- datagrams outside active routes are ignored;
- invalid coordinates, missing fields, invalid timestamps, impossible time deltas, and unrealistic
  segment speeds are rejected;
- output order is deterministic by route and month;
- v1, v2, and v3 produce identical CSV output for the same input and configuration.

## QAW Attribute: Performance

Scenario:

- Source: project evaluator or operations user.
- Stimulus: calculate route-month average speeds over MiniPilot or Pilot datagrams.
- Environment: local files, Java 11, university Linux servers.
- Artifact: speed calculation batch process.
- Response: produce the CSV without manual intervention.
- Measure: elapsed wall-clock time, raw row count, cleaned row count, rejected row count, valid
  segments, output rows.

Priority: high.

Reason: the full yearly dataset is large enough that a sequential baseline is not enough to justify
the final architecture.

## QAW Attribute: Scalability

Scenario:

- Source: project evaluator or operations user.
- Stimulus: increase data volume or number of available processing nodes.
- Environment: university servers with local copies of the dataset.
- Artifact: distributed version.
- Response: distribute route partitions across workers and merge partial results.
- Measure: elapsed time with 1, 2, and 3 processing nodes, plus speedup relative to v1.

Priority: high.

Reason: the project asks to determine when distribution is worth it and to validate the distributed
architecture experimentally.

## Constraints

- Version 1 must be monolithic: one JVM, one local process, no concurrency.
- Version 2 uses a Java `ThreadPool`.
- Version 3 uses a distributed architecture pattern.
- No database is required.
- Runtime network dependency is allowed only for the distributed version.
- Java 11 compatibility is required.

