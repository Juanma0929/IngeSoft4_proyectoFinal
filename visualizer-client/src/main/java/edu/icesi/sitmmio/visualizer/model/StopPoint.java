package edu.icesi.sitmmio.visualizer.model;

public final class StopPoint {
    private final String stopId;
    private final double latitude;
    private final double longitude;
    private final int samples;

    public StopPoint(String stopId, double latitude, double longitude, int samples) {
        this.stopId = stopId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.samples = samples;
    }

    public String stopId() {
        return stopId;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public int samples() {
        return samples;
    }
}

