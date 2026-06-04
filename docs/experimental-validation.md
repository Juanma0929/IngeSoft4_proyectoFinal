# Experimental Validation

This file is the place to record reproducible performance runs. The assignment requires elapsed
times for the monolithic and distributed versions with different numbers of processing nodes.

## Local Review Run

Environment:

- Date: 2026-06-03.
- Machine: local Windows development machine.
- Dataset: `docs/datagrams-MiniPilot.csv`.
- Active routes: `docs/lines-241-ActiveGT.csv`.
- Java/Gradle: project Gradle wrapper.

| Version | Nodes / threads | Raw rows | Clean rows | Rejected rows | Valid segments | Output rows | Runtime ms | Output check |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| v1 monolithic | 1 process | 188183 | 188182 | 1 | 175256 | 111 | 3036 | baseline |
| v2 threadpool | 4 threads | 188183 | 188182 | 1 | 175256 | 111 | 2691 | same SHA-256 as v1 |
| v3 distributed local | 1 worker | 188183 | n/a | n/a | 175256 | 111 | 2989 | same SHA-256 as v1 |

The local v2 result is faster in this run, but MiniPilot is too small to prove scalability. The
required conclusion must be based on the university servers and `datagrams4Pilot.csv`.

## Required Server Runs

Run these on the university infrastructure and paste the results into the table below.

| Version | Processing nodes | Dataset | Runtime ms | Speedup vs v1 | Notes |
|---|---:|---|---:|---:|---|
| v1 monolithic | 1 | datagrams-MiniPilot.csv | pending | 1.00 | baseline |
| v1 monolithic | 1 | datagrams4Pilot.csv | pending | 1.00 | full pilot baseline |
| v3 distributed | 1 worker | datagrams4Pilot.csv | pending | pending | overhead reference |
| v3 distributed | 2 workers | datagrams4Pilot.csv | pending | pending | expected improvement |
| v3 distributed | 3 workers | datagrams4Pilot.csv | pending | pending | scalability point |

## Commands

Monolithic:

```bash
bash scripts/run-monolithic-remote.sh
```

Distributed:

```bash
bash scripts/deploy-v3.sh
```

Correctness check:

```bash
bash scripts/check-real-data-output.sh
```

Java visualization:

```bash
./gradlew visualizer-client:run
```

## Interpretation Rule

Distribution is worth it when the runtime reduction over v1 is greater than the deployment and RMI
coordination overhead. For MiniPilot, the overhead can dominate. For the larger pilot, each route
partition has enough data to make worker parallelism useful.
