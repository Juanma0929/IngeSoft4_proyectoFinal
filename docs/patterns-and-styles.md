# Patterns And Architectural Styles

## Version 1: Monolithic Batch

Style: monolithic batch command-line application.

The v1 module runs in a single JVM and a single process. It reads local files, calculates route-month
aggregates, and writes one deterministic CSV.

Patterns:

- Strategy: `HaversineDistanceCalculator` encapsulates the distance calculation.
- Pipeline-like decomposition: readers, cleaners, segment calculator, aggregator, and writer are
  separate collaborators even though execution is sequential.
- Immutable data records: domain objects are records with explicit fields.

Justification:

This version provides the correctness baseline and the timing baseline required by the assignment.

## Version 2: Thread Pool

Style: shared-memory concurrent batch process.

The main process reads and groups datagrams, then submits one route-oriented task per active route
to an `ExecutorService`.

Patterns:

- Producer-Consumer: the main thread produces route tasks; worker threads consume them.
- Thread Pool: `Executors.newFixedThreadPool(N)` limits and reuses worker threads.
- Strategy: the distance calculator stays replaceable and stateless.

Justification:

Routes are independent after filtering, so route-level parallelism is simple and avoids shared
mutable aggregation state.

## Version 3: Distributed Master-Worker

Style: distributed batch processing with remote workers.

The master partitions active routes and sends each partition to a remote worker through Java RMI.
Each worker reads its local copy of the datagram file, processes only its assigned routes, and
returns partial route-month results. The master reduces the partial outputs into the final CSV.

Patterns:

- Master-Worker: `Master` coordinates partition, map, reduce, and output.
- Remote Proxy: the RMI stub implements `IWorker` and hides remote invocation details.
- Factory Method: `WorkerConnectionFactory` creates worker stubs from `host:port` addresses.

Justification:

The raw CSV is large, so workers read local data instead of transferring datagrams over the network.
Only route partitions and aggregated results cross the network.

