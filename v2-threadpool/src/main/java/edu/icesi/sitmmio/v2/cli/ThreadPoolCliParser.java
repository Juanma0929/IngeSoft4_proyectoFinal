package edu.icesi.sitmmio.v2.cli;

import edu.icesi.sitmmio.cli.CliParser;
import edu.icesi.sitmmio.cli.ParseResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Extiende el parser base extrayendo --threads antes de delegar el resto
 * al CliParser compartido. El argumento --threads no existe en shared para
 * no acoplar versiones anteriores a una opción que solo aplica a v2.
 */
public final class ThreadPoolCliParser {
    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();
    private final CliParser baseParser = new CliParser();

    public ThreadPoolParseResult parse(String[] args) {
        int threadCount = DEFAULT_THREADS;
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                return ThreadPoolParseResult.help();
            }
            if ("--threads".equals(args[i])) {
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    return ThreadPoolParseResult.error("Missing value for option: --threads");
                }
                try {
                    threadCount = Integer.parseInt(args[++i]);
                    if (threadCount < 1) {
                        return ThreadPoolParseResult.error("--threads must be at least 1.");
                    }
                } catch (NumberFormatException e) {
                    return ThreadPoolParseResult.error("--threads must be an integer.");
                }
            } else {
                remaining.add(args[i]);
            }
        }

        ParseResult base = baseParser.parse(remaining.toArray(new String[0]));
        if (base.helpRequested()) {
            return ThreadPoolParseResult.help();
        }
        if (!base.valid()) {
            return ThreadPoolParseResult.error(base.errorMessage());
        }
        return ThreadPoolParseResult.success(new ThreadPoolOptions(base.options(), threadCount));
    }

    public String usage() {
        return baseParser.usage().replace(
                "Version 1 is monolithic: local files, single JVM, no concurrency, no distributed components.\n",
                "  --threads <N>                  Thread pool size. Default: available CPU cores (" + DEFAULT_THREADS + ").\n\n"
                        + "Version 2 uses a fixed ThreadPool (Producer-Consumer pattern).\n"
                        + "Each route is processed independently by a worker thread.\n");
    }
}
