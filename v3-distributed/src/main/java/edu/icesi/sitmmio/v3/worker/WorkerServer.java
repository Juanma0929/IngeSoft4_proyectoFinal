package edu.icesi.sitmmio.v3.worker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Arranca el registro RMI embebido en el puerto indicado y registra un
 * WorkerImpl bajo el nombre "Worker". Bloquea hasta que el proceso sea
 * terminado externamente (Ctrl+C o kill).
 */
public final class WorkerServer {
    private WorkerServer() {
    }

    /**
     * @param port     Puerto del registro RMI (por defecto 1099).
     * @param hostname Hostname que este worker anuncia al master. Si es null
     *                 RMI usa la dirección IP del sistema operativo, lo cual
     *                 funciona correctamente en redes internas universitarias.
     *                 Pasar el hostname explícito cuando haya varias interfaces
     *                 de red o NAT.
     */
    public static void startAndWait(int port, String hostname) throws Exception {
        if (hostname != null && !hostname.isBlank()) {
            // RMI usa esta propiedad para anunciar su dirección en el stub
            // que devuelve al master. Debe ser alcanzable desde el master.
            System.setProperty("java.rmi.server.hostname", hostname);
        }
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("Worker", new WorkerImpl());
        System.out.printf("Worker RMI server ready on port %d (hostname=%s) — waiting for master...%n",
                port, hostname != null ? hostname : "auto");
        Thread.currentThread().join();
    }
}
