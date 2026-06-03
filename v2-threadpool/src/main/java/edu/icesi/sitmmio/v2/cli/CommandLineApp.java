package edu.icesi.sitmmio.v2.cli;

import edu.icesi.sitmmio.v2.service.ThreadPoolRunSummary;
import edu.icesi.sitmmio.v2.service.ThreadPoolSpeedCalculator;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;

public final class CommandLineApp {
    private final PrintStream out;
    private final PrintStream err;
    private final ThreadPoolCliParser parser;
    private final ThreadPoolSpeedCalculator calculator;

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.parser = new ThreadPoolCliParser();
        this.calculator = new ThreadPoolSpeedCalculator();
    }

    public int run(String[] args) {
        ThreadPoolParseResult result = parser.parse(args);
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

        try {
            ThreadPoolRunSummary summary = calculator.run(
                    result.options().base(),
                    result.options().threadCount());
            out.print(summary.formatForConsole());
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("Failed to run threadpool speed calculation: " + e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("Interrupted while waiting for workers: " + e.getMessage());
            return 1;
        } catch (ExecutionException e) {
            err.println("Worker failed: " + e.getCause().getMessage());
            return 1;
        }
    }
}
