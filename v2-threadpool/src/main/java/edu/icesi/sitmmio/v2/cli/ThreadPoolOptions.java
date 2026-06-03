package edu.icesi.sitmmio.v2.cli;

import edu.icesi.sitmmio.cli.CliOptions;

/** Extiende las opciones base con el número de hilos del pool. */
public final class ThreadPoolOptions {
    private final CliOptions base;
    private final int threadCount;

    public ThreadPoolOptions(CliOptions base, int threadCount) {
        if (base == null) {
            throw new IllegalArgumentException("Base options are required.");
        }
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be at least 1.");
        }
        this.base = base;
        this.threadCount = threadCount;
    }

    public CliOptions base() {
        return base;
    }

    public int threadCount() {
        return threadCount;
    }
}
