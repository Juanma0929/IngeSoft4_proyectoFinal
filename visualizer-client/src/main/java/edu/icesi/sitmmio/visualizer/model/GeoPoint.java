package edu.icesi.sitmmio.visualizer.model;

import java.time.LocalTime;

public final class GeoPoint {
    private final double latitude;
    private final double longitude;
    private final LocalTime time;

    public GeoPoint(double latitude, double longitude, LocalTime time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public LocalTime time() {
        return time;
    }
}

