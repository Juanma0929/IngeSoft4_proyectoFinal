package edu.icesi.sitmmio.v3.cli;

import edu.icesi.sitmmio.v3.master.Master;
import edu.icesi.sitmmio.v3.master.MasterRunSummary;
import edu.icesi.sitmmio.v3.worker.WorkerServer;

import java.io.PrintStream;

public final class CommandLineApp {
    private final PrintStream out;
    private final PrintStream err;
    private final V3CliParser parser;

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.parser = new V3CliParser();
    }

    public int run(String[] args) {
        V3ParseResult result = parser.parse(args);
        if (result.helpRequested()) {
            out.print(parser.usage());
            return 0;
        }
        if (!result.valid()) {
            err.println(result.errorMessage());
            err.println();
            err.print(parser.usage());
            return 2;
        }

        V3CliOptions options = result.options();

        if (options.mode() == V3CliOptions.Mode.WORKER) {
            return runWorker(options);
        }

        return runMaster(options);
    }

    private int runWorker(V3CliOptions options) {
        try {
            WorkerServer.startAndWait(options.port(), options.hostname());
            return 0;
        } catch (Exception e) {
            err.println("Worker failed to start: " + e.getMessage());
            return 1;
        }
    }

    private int runMaster(V3CliOptions options) {
        try {
            Master master = new Master();
            MasterRunSummary summary = master.run(
                    options.masterBase(), options.workerAddresses());
            out.print(summary.formatForConsole());
            return 0;
        } catch (Exception e) {
            err.println("Master failed: " + e.getMessage());
            return 1;
        }
    }
}
