# Version 3 Deployment

The distributed version uses Java RMI and the Master-Worker pattern. The deployment assumes that
each processing server can read the assignment CSV files from its local filesystem path.

```mermaid
flowchart LR
    subgraph "Master node: 104m03"
        M["Master CLI\nv3-distributed --mode master"]
        R["Reduce + CSV writer"]
        O["~/results/v3-pilot.csv"]
        M --> R
        R --> O
    end

    subgraph "Worker node: 205m03"
        W1["Worker RMI server\nv3-distributed --mode worker"]
        D1["/opt/sitm-mio/datagrams4Pilot.csv"]
        W1 --> D1
    end

    subgraph "Worker node: 206m03"
        W2["Worker RMI server\nv3-distributed --mode worker"]
        D2["/opt/sitm-mio/datagrams4Pilot.csv"]
        W2 --> D2
    end

    L["/opt/sitm-mio/lines-241-ActiveGT.csv"] --> M
    M -- "RMI map(route partition)" --> W1
    M -- "RMI map(route partition)" --> W2
    W1 -- "partial route-month speeds" --> M
    W2 -- "partial route-month speeds" --> M
```

## Runtime Responsibilities

- Master:
  - reads active routes;
  - partitions routes evenly by worker count;
  - connects to workers with RMI;
  - dispatches one partition per worker;
  - merges partial results;
  - writes the final CSV.

- Worker:
  - exposes `IWorker.map()` over RMI;
  - receives assigned routes and CSV configuration;
  - reads the datagram file locally;
  - filters to assigned routes;
  - calculates segments and aggregates by route-month;
  - returns partial aggregates to the master.

## Deployment Command

Use:

```bash
bash scripts/deploy-v3.sh
```

The script builds the v3 distribution, copies it to the master and worker hosts, starts workers,
runs the master, and stops the workers.

