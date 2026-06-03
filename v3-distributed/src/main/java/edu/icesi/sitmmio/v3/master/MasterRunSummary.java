package edu.icesi.sitmmio.v3.master;

import java.nio.file.Path;
import java.util.Locale;

public final class MasterRunSummary {
    private final Path outputPath;
    private final int workerCount;
    private final int activeRoutes;
    private final int totalSegments;
    private final int outputRows;
    private final long runtimeMillis;

    public MasterRunSummary(
            Path outputPath,
            int workerCount,
            int activeRoutes,
            int totalSegments,
            int outputRows,
            long runtimeMillis
    ) {
        this.outputPath = outputPath;
        this.workerCount = workerCount;
        this.activeRoutes = activeRoutes;
        this.totalSegments = totalSegments;
        this.outputRows = outputRows;
        this.runtimeMillis = runtimeMillis;
    }

    public Path outputPath() { return outputPath; }
    public int workerCount() { return workerCount; }
    public int activeRoutes() { return activeRoutes; }
    public int totalSegments() { return totalSegments; }
    public int outputRows() { return outputRows; }
    public long runtimeMillis() { return runtimeMillis; }

    public String formatForConsole() {
        String template = "SITM-MIO distributed speed calculator (Master-Worker) completed%n"
                + "Workers used:    %d%n"
                + "Active routes:   %d%n"
                + "Total segments:  %d%n"
                + "Output rows:     %d%n"
                + "Total runtime ms: %d%n"
                + "Output CSV:      %s%n";
        return String.format(Locale.ROOT, template,
                workerCount, activeRoutes, totalSegments, outputRows, runtimeMillis, outputPath);
    }
}
