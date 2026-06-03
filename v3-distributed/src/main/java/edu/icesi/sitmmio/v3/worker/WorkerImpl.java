package edu.icesi.sitmmio.v3.worker;

import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.GpsDatagramCsvReader;
import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;
import edu.icesi.sitmmio.service.RouteMonthAggregator;
import edu.icesi.sitmmio.service.SpeedSegmentCalculator;
import edu.icesi.sitmmio.v3.rmi.IWorker;
import edu.icesi.sitmmio.v3.rmi.WorkerRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Patrón Master-Worker — rol Worker.
 *
 * Implementa IWorker via RMI. El Master invoca map() de forma remota;
 * el stub RMI actúa como Proxy: serializa WorkerRequest, lo envía por
 * red, y deserializa List&lt;RouteMonthSpeed&gt; de vuelta al Master.
 *
 * El Worker es completamente autónomo: lee el CSV en su propia máquina,
 * sin que el Master transfiera los datos crudos por red.
 */
public final class WorkerImpl extends UnicastRemoteObject implements IWorker {
    private static final long serialVersionUID = 1L;

    public WorkerImpl() throws RemoteException {
        super();
    }

    /**
     * Fase MAP: lee el CSV local, filtra a las rutas asignadas, calcula
     * segmentos de velocidad y agrega por ruta-mes. Retorna resultados
     * parciales al Master para la fase REDUCE.
     */
    @Override
    public List<RouteMonthSpeed> map(WorkerRequest request) throws RemoteException {
        try {
            Set<String> assignedRoutes = new HashSet<>(request.assignedRoutes());

            CsvColumnConfig columnConfig = new CsvColumnConfig(
                    null,
                    request.routeColumn(),
                    request.busColumn(),
                    request.timestampColumn(),
                    request.latitudeColumn(),
                    request.longitudeColumn(),
                    request.datagramsHasHeader(),
                    request.routeIndex(),
                    request.busIndex(),
                    request.timestampIndex(),
                    request.latitudeIndex(),
                    request.longitudeIndex(),
                    request.coordinateScale());

            List<GpsPoint> points = new GpsDatagramCsvReader().read(
                    Path.of(request.datagramsPath()), assignedRoutes, columnConfig);

            List<GpsPoint> sorted = points.stream()
                    .sorted(Comparator.comparing(GpsPoint::routeId)
                            .thenComparing(GpsPoint::busId)
                            .thenComparing(GpsPoint::timestamp))
                    .collect(Collectors.toList());

            SpeedSegmentCalculator segCalc = new SpeedSegmentCalculator(
                    new HaversineDistanceCalculator(),
                    Duration.ofMinutes(request.maxGapMinutes()),
                    request.maxSpeedKmh());

            List<SpeedSegment> segments = segCalc.calculateSegments(sorted);
            return new ArrayList<>(new RouteMonthAggregator().aggregate(segments));

        } catch (IOException e) {
            throw new RemoteException("Worker map() failed: " + e.getMessage(), e);
        }
    }
}
