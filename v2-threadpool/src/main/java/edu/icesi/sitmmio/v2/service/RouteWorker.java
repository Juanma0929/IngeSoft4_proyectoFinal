package edu.icesi.sitmmio.v2.service;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;
import edu.icesi.sitmmio.service.RouteMonthAggregator;
import edu.icesi.sitmmio.service.SpeedSegmentCalculator;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Patrón Producer-Consumer — rol Consumer.
 *
 * Cada RouteWorker es una unidad de trabajo independiente que procesa todos
 * los datagramas de UNA ruta. El ThreadPoolSpeedCalculator (Producer) crea
 * una instancia por ruta y las entrega al ExecutorService (cola interna del
 * pool). Los hilos del pool consumen y ejecutan call() en paralelo.
 */
public final class RouteWorker implements Callable<List<RouteMonthSpeed>> {
    private final String routeId;
    private final List<GpsPoint> points;
    private final SpeedSegmentCalculator segmentCalculator;
    private final RouteMonthAggregator aggregator;

    public RouteWorker(
            String routeId,
            List<GpsPoint> points,
            HaversineDistanceCalculator distanceCalculator,
            Duration maxGap,
            double maxSpeedKmh
    ) {
        this.routeId = routeId;
        this.points = points;
        this.segmentCalculator = new SpeedSegmentCalculator(distanceCalculator, maxGap, maxSpeedKmh);
        this.aggregator = new RouteMonthAggregator();
    }

    /** Devuelve las velocidades promedio por mes para esta ruta. */
    @Override
    public List<RouteMonthSpeed> call() {
        if (points.isEmpty()) {
            return List.of();
        }

        List<GpsPoint> sorted = points.stream()
                .sorted(Comparator.comparing(GpsPoint::busId).thenComparing(GpsPoint::timestamp))
                .collect(Collectors.toList());

        List<SpeedSegment> segments = segmentCalculator.calculateSegments(sorted);
        return aggregator.aggregate(segments);
    }

    public String routeId() {
        return routeId;
    }
}
