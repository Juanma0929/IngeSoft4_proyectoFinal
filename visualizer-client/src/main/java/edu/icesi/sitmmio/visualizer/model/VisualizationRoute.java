package edu.icesi.sitmmio.visualizer.model;

import java.util.List;

public final class VisualizationRoute {
    private final String routeId;
    private final String shortName;
    private final String description;
    private final SpeedMetrics metrics;
    private final List<GeoPoint> routePath;
    private final List<StopPoint> stops;
    private final List<BusTrack> buses;
    private final boolean testRoute;

    public VisualizationRoute(
            String routeId,
            String shortName,
            String description,
            SpeedMetrics metrics,
            List<GeoPoint> routePath,
            List<StopPoint> stops,
            List<BusTrack> buses,
            boolean testRoute
    ) {
        this.routeId = routeId;
        this.shortName = shortName;
        this.description = description;
        this.metrics = metrics;
        this.routePath = List.copyOf(routePath);
        this.stops = List.copyOf(stops);
        this.buses = List.copyOf(buses);
        this.testRoute = testRoute;
    }

    public String routeId() {
        return routeId;
    }

    public String shortName() {
        return shortName;
    }

    public String description() {
        return description;
    }

    public SpeedMetrics metrics() {
        return metrics;
    }

    public List<GeoPoint> routePath() {
        return routePath;
    }

    public List<StopPoint> stops() {
        return stops;
    }

    public List<BusTrack> buses() {
        return buses;
    }

    public boolean testRoute() {
        return testRoute;
    }

    @Override
    public String toString() {
        return shortName + " (" + routeId + ")";
    }
}
