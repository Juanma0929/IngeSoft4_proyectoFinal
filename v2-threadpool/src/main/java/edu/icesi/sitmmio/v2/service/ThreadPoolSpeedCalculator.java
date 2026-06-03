package edu.icesi.sitmmio.v2.service;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.GpsDatagramCsvReader;
import edu.icesi.sitmmio.csv.GpsDatagramReadResult;
import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
 * Patrón Producer-Consumer con ThreadPool.
 *
 * PRODUCER (hilo principal):
 *   Lee el CSV completo y agrupa los GpsPoints por routeId. Por cada ruta
 *   crea un RouteWorker (Callable) y lo envía al ExecutorService.
 *
 * CONSUMER (hilos del pool):
 *   Cada RouteWorker toma su lista de GpsPoints, calcula segmentos de
 *   velocidad y agrega por mes. Los workers son completamente independientes
 *   entre sí: no comparten estado mutable.
 *
 * MERGE (hilo principal):
 *   Recoge los Future<List<RouteMonthSpeed>> de todos los workers, fusiona
 *   los resultados parciales y rellena las combinaciones ruta-mes sin datos.
 */
public final class ThreadPoolSpeedCalculator {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final GpsDatagramCsvReader datagramReader;
    private final ResultCsvWriter resultWriter;
    private final HaversineDistanceCalculator distanceCalculator;

    public ThreadPoolSpeedCalculator() {
        this(
                new ActiveRoutesCsvReader(),
                new GpsDatagramCsvReader(),
                new ResultCsvWriter(),
                new HaversineDistanceCalculator());
    }

    ThreadPoolSpeedCalculator(
            ActiveRoutesCsvReader activeRoutesReader,
            GpsDatagramCsvReader datagramReader,
            ResultCsvWriter resultWriter,
            HaversineDistanceCalculator distanceCalculator
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.datagramReader = datagramReader;
        this.resultWriter = resultWriter;
        this.distanceCalculator = distanceCalculator;
    }

    public ThreadPoolRunSummary run(CliOptions options, int threadCount)
            throws IOException, InterruptedException, ExecutionException {
        long startNanos = System.nanoTime();

        // --- PRODUCER: lectura y partición ---
        Set<String> activeRoutes = activeRoutesReader.read(
                options.linesPath(), options.activeRouteColumn());

        CsvColumnConfig columnConfig = new CsvColumnConfig(
                options.activeRouteColumn(),
                options.routeColumn(),
                options.busColumn(),
                options.timestampColumn(),
                options.latitudeColumn(),
                options.longitudeColumn(),
                options.datagramsHasHeader(),
                options.routeIndex(),
                options.busIndex(),
                options.timestampIndex(),
                options.latitudeIndex(),
                options.longitudeIndex(),
                options.coordinateScale());

        GpsDatagramReadResult readResult = datagramReader.readWithStats(
                options.datagramsPath(), activeRoutes, columnConfig);

        Map<String, List<GpsPoint>> byRoute = groupByRoute(readResult.cleanedDatagrams());

        Duration maxGap = Duration.ofMinutes(options.maxGapMinutes());
        double maxSpeedKmh = options.maxSpeedKmh();

        // --- PRODUCER: crea un worker por ruta y los envía al pool ---
        List<Callable<List<RouteMonthSpeed>>> workers = new ArrayList<>(activeRoutes.size());
        for (String routeId : activeRoutes) {
            List<GpsPoint> points = byRoute.getOrDefault(routeId, Collections.emptyList());
            workers.add(new RouteWorker(routeId, points, distanceCalculator, maxGap, maxSpeedKmh));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        // invokeAll bloquea hasta que todos los workers terminan
        List<Future<List<RouteMonthSpeed>>> futures = pool.invokeAll(workers);
        pool.shutdown();

        // --- MERGE: recoge resultados parciales de cada worker ---
        List<RouteMonthSpeed> allResults = new ArrayList<>();
        for (Future<List<RouteMonthSpeed>> future : futures) {
            allResults.addAll(future.get());
        }

        Set<YearMonth> months = detectedMonths(allResults);
        List<RouteMonthSpeed> completed = completeActiveRouteMonths(activeRoutes, months, allResults);
        resultWriter.write(options.outputPath(), completed);

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        int totalSegments = allResults.stream().mapToInt(r -> (int) r.validSegments()).sum();
        return new ThreadPoolRunSummary(
                options.outputPath(),
                threadCount,
                activeRoutes.size(),
                readResult.rawDatagrams(),
                readResult.cleanedDatagrams().size(),
                readResult.skippedInvalidRows(),
                totalSegments,
                completed.size(),
                runtimeMillis);
    }

    private static Map<String, List<GpsPoint>> groupByRoute(List<GpsPoint> points) {
        Map<String, List<GpsPoint>> grouped = new HashMap<>();
        for (GpsPoint point : points) {
            grouped.computeIfAbsent(point.routeId(), ignored -> new ArrayList<>()).add(point);
        }
        return grouped;
    }

    private static Set<YearMonth> detectedMonths(List<RouteMonthSpeed> results) {
        Set<YearMonth> months = new TreeSet<>();
        for (RouteMonthSpeed result : results) {
            months.add(result.month());
        }
        return months;
    }

    private static List<RouteMonthSpeed> completeActiveRouteMonths(
            Set<String> activeRoutes,
            Set<YearMonth> months,
            List<RouteMonthSpeed> results
    ) {
        Map<RouteMonthKey, RouteMonthSpeed> byKey = new HashMap<>();
        for (RouteMonthSpeed result : results) {
            byKey.put(new RouteMonthKey(result.routeId(), result.month()), result);
        }

        return new TreeSet<>(activeRoutes).stream()
                .flatMap(route -> months.stream()
                        .map(month -> byKey.getOrDefault(
                                new RouteMonthKey(route, month),
                                emptyResult(route, month))))
                .sorted(Comparator.comparing(RouteMonthSpeed::routeId).thenComparing(RouteMonthSpeed::month))
                .collect(Collectors.toList());
    }

    private static RouteMonthSpeed emptyResult(String routeId, YearMonth month) {
        return new RouteMonthSpeed(routeId, month, 0.0, 0.0, 0.0, 0.0, 0, 0);
    }

    private static final class RouteMonthKey {
        private final String routeId;
        private final YearMonth month;

        private RouteMonthKey(String routeId, YearMonth month) {
            this.routeId = routeId;
            this.month = month;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RouteMonthKey)) {
                return false;
            }
            RouteMonthKey that = (RouteMonthKey) other;
            return routeId.equals(that.routeId) && month.equals(that.month);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeId, month);
        }
    }
}
