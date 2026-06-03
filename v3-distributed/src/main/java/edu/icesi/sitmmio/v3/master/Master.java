package edu.icesi.sitmmio.v3.master;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;
import edu.icesi.sitmmio.v3.rmi.IWorker;
import edu.icesi.sitmmio.v3.rmi.WorkerRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Patrón Master-Worker (distribución).
 *
 * El Master coordina la computación distribuida en cuatro fases:
 *
 *  1. PARTITION  — Divide las rutas activas en N grupos iguales (uno por Worker).
 *  2. MAP        — Envía cada grupo al Worker correspondiente vía RMI en paralelo.
 *                  Cada Worker lee el CSV en su propia máquina y retorna
 *                  resultados parciales. Los Workers son independientes entre sí.
 *  3. REDUCE     — El Master fusiona todos los resultados parciales en un único
 *                  mapa ruta-mes → velocidad promedio.
 *  4. OUTPUT     — Completa combinaciones ruta-mes sin datos y escribe el CSV final.
 */
public final class Master {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final WorkerConnectionFactory workerFactory;
    private final ResultCsvWriter resultWriter;

    public Master() {
        this(new ActiveRoutesCsvReader(), new WorkerConnectionFactory(), new ResultCsvWriter());
    }

    Master(ActiveRoutesCsvReader activeRoutesReader,
           WorkerConnectionFactory workerFactory,
           ResultCsvWriter resultWriter) {
        this.activeRoutesReader = activeRoutesReader;
        this.workerFactory = workerFactory;
        this.resultWriter = resultWriter;
    }

    public MasterRunSummary run(CliOptions options, List<WorkerAddress> workerAddresses)
            throws IOException, InterruptedException, ExecutionException,
            java.rmi.RemoteException, java.rmi.NotBoundException {
        long startNanos = System.nanoTime();

        // --- PARTITION ---
        Set<String> activeRoutes = activeRoutesReader.read(
                options.linesPath(), options.activeRouteColumn());
        List<String> routeList = new ArrayList<>(activeRoutes);
        Collections.sort(routeList);

        List<IWorker> workers = workerFactory.connectAll(workerAddresses);
        List<List<String>> partitions = partitionRoutes(routeList, workers.size());

        // --- MAP (paralelo): un hilo por worker ---
        ExecutorService pool = Executors.newFixedThreadPool(workers.size());
        List<Callable<List<RouteMonthSpeed>>> tasks = new ArrayList<>(workers.size());
        for (int i = 0; i < workers.size(); i++) {
            final IWorker worker = workers.get(i);
            final WorkerRequest request = WorkerRequest.from(options, partitions.get(i));
            final int idx = i;
            tasks.add(() -> {
                System.out.printf("Dispatching %d routes to worker %d...%n",
                        request.assignedRoutes().size(), idx);
                List<RouteMonthSpeed> partial = worker.map(request);
                System.out.printf("Worker %d returned %d results%n", idx, partial.size());
                return partial;
            });
        }

        List<Future<List<RouteMonthSpeed>>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        // --- REDUCE ---
        List<RouteMonthSpeed> allResults = new ArrayList<>();
        for (Future<List<RouteMonthSpeed>> future : futures) {
            allResults.addAll(future.get());
        }

        Set<YearMonth> months = detectedMonths(allResults);
        List<RouteMonthSpeed> completed = completeActiveRouteMonths(activeRoutes, months, allResults);
        resultWriter.write(options.outputPath(), completed);

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        int totalSegments = allResults.stream().mapToInt(r -> (int) r.validSegments()).sum();
        return new MasterRunSummary(
                options.outputPath(),
                workers.size(),
                activeRoutes.size(),
                totalSegments,
                completed.size(),
                runtimeMillis);
    }

    /** Divide routeList en numWorkers grupos lo más iguales posible. */
    private static List<List<String>> partitionRoutes(List<String> routes, int numWorkers) {
        List<List<String>> partitions = new ArrayList<>(numWorkers);
        int total = routes.size();
        int base = total / numWorkers;
        int extra = total % numWorkers;
        int offset = 0;
        for (int i = 0; i < numWorkers; i++) {
            int size = base + (i < extra ? 1 : 0);
            partitions.add(new ArrayList<>(routes.subList(offset, offset + size)));
            offset += size;
        }
        return partitions;
    }

    private static Set<YearMonth> detectedMonths(List<RouteMonthSpeed> results) {
        Set<YearMonth> months = new TreeSet<>();
        for (RouteMonthSpeed r : results) {
            months.add(r.month());
        }
        return months;
    }

    private static List<RouteMonthSpeed> completeActiveRouteMonths(
            Set<String> activeRoutes, Set<YearMonth> months, List<RouteMonthSpeed> results) {
        Map<RouteMonthKey, RouteMonthSpeed> byKey = new HashMap<>();
        for (RouteMonthSpeed r : results) {
            byKey.put(new RouteMonthKey(r.routeId(), r.month()), r);
        }
        return new TreeSet<>(activeRoutes).stream()
                .flatMap(route -> months.stream()
                        .map(month -> byKey.getOrDefault(
                                new RouteMonthKey(route, month),
                                emptyResult(route, month))))
                .sorted(Comparator.comparing(RouteMonthSpeed::routeId)
                        .thenComparing(RouteMonthSpeed::month))
                .collect(Collectors.toList());
    }

    private static RouteMonthSpeed emptyResult(String routeId, YearMonth month) {
        return new RouteMonthSpeed(routeId, month, 0.0, 0.0, 0.0, 0.0, 0, 0);
    }

    private static final class RouteMonthKey {
        private final String routeId;
        private final YearMonth month;

        RouteMonthKey(String routeId, YearMonth month) {
            this.routeId = routeId;
            this.month = month;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RouteMonthKey)) return false;
            RouteMonthKey that = (RouteMonthKey) o;
            return routeId.equals(that.routeId) && month.equals(that.month);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeId, month);
        }
    }
}
