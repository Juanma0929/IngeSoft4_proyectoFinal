package edu.icesi.sitmmio.v2;

import edu.icesi.sitmmio.v2.cli.CommandLineApp;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLineApp(System.out, System.err).run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
