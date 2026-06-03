package edu.icesi.sitmmio.v3.master;

import edu.icesi.sitmmio.v3.rmi.IWorker;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Factory Method.
 *
 * Crea instancias de IWorker (stubs RMI) a partir de direcciones host:port.
 * El Master usa esta factory sin conocer si los Workers son locales o
 * remotos, ni los detalles del protocolo de conexión RMI.
 */
public final class WorkerConnectionFactory {

    /** Conecta a un Worker individual y retorna su stub RMI. */
    public IWorker connect(WorkerAddress address) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(address.host(), address.port());
        return (IWorker) registry.lookup("Worker");
    }

    /** Conecta a todos los Workers y retorna sus stubs en orden. */
    public List<IWorker> connectAll(List<WorkerAddress> addresses)
            throws RemoteException, NotBoundException {
        List<IWorker> workers = new ArrayList<>(addresses.size());
        for (WorkerAddress address : addresses) {
            System.out.printf("Connecting to worker at %s...%n", address);
            workers.add(connect(address));
            System.out.printf("Connected to worker at %s%n", address);
        }
        return workers;
    }
}
