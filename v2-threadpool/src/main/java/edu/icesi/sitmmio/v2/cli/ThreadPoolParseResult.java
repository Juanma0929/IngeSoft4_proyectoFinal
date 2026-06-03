package edu.icesi.sitmmio.v2.cli;

public final class ThreadPoolParseResult {
    private final ThreadPoolOptions options;
    private final String errorMessage;
    private final boolean helpRequested;

    private ThreadPoolParseResult(ThreadPoolOptions options, String errorMessage, boolean helpRequested) {
        this.options = options;
        this.errorMessage = errorMessage;
        this.helpRequested = helpRequested;
    }

    public static ThreadPoolParseResult success(ThreadPoolOptions options) {
        return new ThreadPoolParseResult(options, null, false);
    }

    public static ThreadPoolParseResult error(String message) {
        return new ThreadPoolParseResult(null, message, false);
    }

    public static ThreadPoolParseResult help() {
        return new ThreadPoolParseResult(null, null, true);
    }

    public boolean valid() {
        return options != null;
    }

    public boolean helpRequested() {
        return helpRequested;
    }

    public ThreadPoolOptions options() {
        return options;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
