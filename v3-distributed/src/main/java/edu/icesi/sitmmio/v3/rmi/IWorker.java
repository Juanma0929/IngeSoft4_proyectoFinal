package edu.icesi.sitmmio.v3.rmi;

import edu.icesi.sitmmio.model.RouteMonthSpeed;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Patrón Master-Worker — contrato del Worker.
 *
 * Define el único método que el Master invoca en cada Worker remoto.
 * El stub RMI generado automáticamente actúa como Proxy: el Master llama
 * map() como si fuera un objeto local; RMI serializa la solicitud, la
 * envía por red y deserializa la respuesta, de forma transparente.
 */
public interface IWorker extends Remote {

    /**
     * Fase MAP del patrón Master-Worker.
     *
     * El Worker lee el CSV en su propia máquina, filtra a las rutas
     * asignadas, calcula velocidades y retorna resultados parciales.
     */
    List<RouteMonthSpeed> map(WorkerRequest request) throws RemoteException;
}
