package edu.icesi.sitmmio.v3.master;

/** Dirección de un Worker RMI: host y puerto del registro. */
public final class WorkerAddress {
    private final String host;
    private final int port;

    public WorkerAddress(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Worker host is required.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Worker port must be between 1 and 65535.");
        }
        this.host = host;
        this.port = port;
    }

    /** Parsea una cadena con formato "host:port". */
    public static WorkerAddress parse(String hostPort) {
        int colon = hostPort.lastIndexOf(':');
        if (colon < 1) {
            throw new IllegalArgumentException("Worker address must be host:port, got: " + hostPort);
        }
        String host = hostPort.substring(0, colon);
        int port;
        try {
            port = Integer.parseInt(hostPort.substring(colon + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port in worker address: " + hostPort, e);
        }
        return new WorkerAddress(host, port);
    }

    public String host() { return host; }
    public int port() { return port; }

    @Override
    public String toString() { return host + ":" + port; }
}
