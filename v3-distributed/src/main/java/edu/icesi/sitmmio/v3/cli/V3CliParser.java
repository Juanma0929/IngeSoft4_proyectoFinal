package edu.icesi.sitmmio.v3.cli;

import edu.icesi.sitmmio.cli.CliParser;
import edu.icesi.sitmmio.cli.ParseResult;
import edu.icesi.sitmmio.v3.master.WorkerAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser de argumentos para v3. Extrae --mode, --workers y --port antes
 * de delegar los argumentos restantes al CliParser compartido (modo master).
 */
public final class V3CliParser {
    private static final int DEFAULT_WORKER_PORT = 1099;
    private final CliParser baseParser = new CliParser();

    public V3ParseResult parse(String[] args) {
        String mode = null;
        String workersStr = null;
        int port = DEFAULT_WORKER_PORT;
        String hostname = null;
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return V3ParseResult.help();
            }
            if ("--mode".equals(arg)) {
                if (i + 1 >= args.length) {
                    return V3ParseResult.error("Missing value for --mode.");
                }
                mode = args[++i];
            } else if ("--workers".equals(arg)) {
                if (i + 1 >= args.length) {
                    return V3ParseResult.error("Missing value for --workers.");
                }
                workersStr = args[++i];
            } else if ("--port".equals(arg)) {
                if (i + 1 >= args.length) {
                    return V3ParseResult.error("Missing value for --port.");
                }
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    return V3ParseResult.error("--port must be an integer.");
                }
            } else if ("--hostname".equals(arg)) {
                if (i + 1 >= args.length) {
                    return V3ParseResult.error("Missing value for --hostname.");
                }
                hostname = args[++i];
            } else {
                remaining.add(arg);
            }
        }

        if (mode == null) {
            return V3ParseResult.error("--mode is required. Use 'master' or 'worker'.");
        }

        if ("worker".equalsIgnoreCase(mode)) {
            return V3ParseResult.success(new V3CliOptions(port, hostname));
        }

        if ("master".equalsIgnoreCase(mode)) {
            if (workersStr == null || workersStr.isBlank()) {
                return V3ParseResult.error("--workers is required in master mode. "
                        + "Format: host1:port1,host2:port2");
            }
            List<WorkerAddress> addresses;
            try {
                addresses = parseWorkerAddresses(workersStr);
            } catch (IllegalArgumentException e) {
                return V3ParseResult.error("Invalid --workers value: " + e.getMessage());
            }
            ParseResult base = baseParser.parse(remaining.toArray(new String[0]));
            if (base.helpRequested()) {
                return V3ParseResult.help();
            }
            if (!base.valid()) {
                return V3ParseResult.error(base.errorMessage());
            }
            return V3ParseResult.success(new V3CliOptions(base.options(), addresses));
        }

        return V3ParseResult.error("--mode must be 'master' or 'worker', got: " + mode);
    }

    public String usage() {
        return "Version 3 — Master-Worker distributed via Java RMI\n\n"
                + "Worker mode (run on each remote server):\n"
                + "  --mode worker\n"
                + "  --port <N>        RMI registry port. Default: " + DEFAULT_WORKER_PORT + "\n"
                + "  --hostname <name> Hostname que el worker anuncia al master. Default: auto\n\n"
                + "Master mode (run once, after all workers are ready):\n"
                + "  --mode master\n"
                + "  --workers <host1:port1,host2:port2,...>   Worker addresses (required)\n\n"
                + baseParser.usage().replace(
                        "Version 1 is monolithic: local files, single JVM, "
                                + "no concurrency, no distributed components.\n", "");
    }

    private static List<WorkerAddress> parseWorkerAddresses(String workersStr) {
        String[] parts = workersStr.split(",");
        List<WorkerAddress> addresses = new ArrayList<>(parts.length);
        for (String part : parts) {
            addresses.add(WorkerAddress.parse(part.trim()));
        }
        return addresses;
    }
}
