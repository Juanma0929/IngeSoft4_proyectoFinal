package edu.icesi.sitmmio.visualizer.model;

import java.util.List;

public final class BusTrack {
    private final String busId;
    private final List<GeoPoint> points;

    public BusTrack(String busId, List<GeoPoint> points) {
        this.busId = busId;
        this.points = List.copyOf(points);
    }

    public String busId() {
        return busId;
    }

    public List<GeoPoint> points() {
        return points;
    }
}

