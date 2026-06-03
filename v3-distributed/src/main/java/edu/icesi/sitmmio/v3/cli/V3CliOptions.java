package edu.icesi.sitmmio.v3.cli;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.v3.master.WorkerAddress;

import java.util.List;

public final class V3CliOptions {

    public enum Mode { MASTER, WORKER }

    private final Mode mode;
    private final CliOptions masterBase;
    private final List<WorkerAddress> workerAddresses;
    private final int port;
    private final String hostname;

    /** Constructor para modo master. */
    public V3CliOptions(CliOptions masterBase, List<WorkerAddress> workerAddresses) {
        this.mode = Mode.MASTER;
        this.masterBase = masterBase;
        this.workerAddresses = workerAddresses;
        this.port = 0;
        this.hostname = null;
    }

    /** Constructor para modo worker. */
    public V3CliOptions(int port, String hostname) {
        this.mode = Mode.WORKER;
        this.masterBase = null;
        this.workerAddresses = null;
        this.port = port;
        this.hostname = hostname;
    }

    public Mode mode() { return mode; }
    public CliOptions masterBase() { return masterBase; }
    public List<WorkerAddress> workerAddresses() { return workerAddresses; }
    public int port() { return port; }
    /** Hostname que el worker anuncia al master vía RMI. Null = auto-detectar. */
    public String hostname() { return hostname; }
}
