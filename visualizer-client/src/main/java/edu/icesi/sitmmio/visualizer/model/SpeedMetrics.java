package edu.icesi.sitmmio.visualizer.model;

public final class SpeedMetrics {
    private final String month;
    private final double averageSpeedKmh;
    private final long validSegments;
    private final long busesObserved;

    public SpeedMetrics(String month, double averageSpeedKmh, long validSegments, long busesObserved) {
        this.month = month;
        this.averageSpeedKmh = averageSpeedKmh;
        this.validSegments = validSegments;
        this.busesObserved = busesObserved;
    }

    public String month() {
        return month;
    }

    public double averageSpeedKmh() {
        return averageSpeedKmh;
    }

    public long validSegments() {
        return validSegments;
    }

    public long busesObserved() {
        return busesObserved;
    }
}

