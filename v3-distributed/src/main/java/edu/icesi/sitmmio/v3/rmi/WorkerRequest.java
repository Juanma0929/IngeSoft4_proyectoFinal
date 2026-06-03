package edu.icesi.sitmmio.v3.rmi;

import edu.icesi.sitmmio.cli.CliOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de transferencia serializable que el Master envía a cada Worker.
 * Contiene el subconjunto de rutas asignadas y toda la configuración
 * necesaria para que el Worker lea y procese el CSV de forma autónoma.
 */
public final class WorkerRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> assignedRoutes;
    private final String datagramsPath;
    private final boolean datagramsHasHeader;
    private final Integer routeIndex;
    private final Integer busIndex;
    private final Integer timestampIndex;
    private final Integer latitudeIndex;
    private final Integer longitudeIndex;
    private final double coordinateScale;
    private final String routeColumn;
    private final String busColumn;
    private final String timestampColumn;
    private final String latitudeColumn;
    private final String longitudeColumn;
    private final int maxGapMinutes;
    private final double maxSpeedKmh;

    public static WorkerRequest from(CliOptions options, List<String> assignedRoutes) {
        return new WorkerRequest(
                new ArrayList<>(assignedRoutes),
                options.datagramsPath().toString(),
                options.datagramsHasHeader(),
                options.routeIndex(),
                options.busIndex(),
                options.timestampIndex(),
                options.latitudeIndex(),
                options.longitudeIndex(),
                options.coordinateScale(),
                options.routeColumn(),
                options.busColumn(),
                options.timestampColumn(),
                options.latitudeColumn(),
                options.longitudeColumn(),
                options.maxGapMinutes(),
                options.maxSpeedKmh());
    }

    private WorkerRequest(
            ArrayList<String> assignedRoutes,
            String datagramsPath,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        this.assignedRoutes = assignedRoutes;
        this.datagramsPath = datagramsPath;
        this.datagramsHasHeader = datagramsHasHeader;
        this.routeIndex = routeIndex;
        this.busIndex = busIndex;
        this.timestampIndex = timestampIndex;
        this.latitudeIndex = latitudeIndex;
        this.longitudeIndex = longitudeIndex;
        this.coordinateScale = coordinateScale;
        this.routeColumn = routeColumn;
        this.busColumn = busColumn;
        this.timestampColumn = timestampColumn;
        this.latitudeColumn = latitudeColumn;
        this.longitudeColumn = longitudeColumn;
        this.maxGapMinutes = maxGapMinutes;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public List<String> assignedRoutes() { return assignedRoutes; }
    public String datagramsPath() { return datagramsPath; }
    public boolean datagramsHasHeader() { return datagramsHasHeader; }
    public Integer routeIndex() { return routeIndex; }
    public Integer busIndex() { return busIndex; }
    public Integer timestampIndex() { return timestampIndex; }
    public Integer latitudeIndex() { return latitudeIndex; }
    public Integer longitudeIndex() { return longitudeIndex; }
    public double coordinateScale() { return coordinateScale; }
    public String routeColumn() { return routeColumn; }
    public String busColumn() { return busColumn; }
    public String timestampColumn() { return timestampColumn; }
    public String latitudeColumn() { return latitudeColumn; }
    public String longitudeColumn() { return longitudeColumn; }
    public int maxGapMinutes() { return maxGapMinutes; }
    public double maxSpeedKmh() { return maxSpeedKmh; }
}
