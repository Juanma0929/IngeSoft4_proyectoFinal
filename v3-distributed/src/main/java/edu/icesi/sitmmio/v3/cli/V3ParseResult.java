package edu.icesi.sitmmio.v3.cli;

public final class V3ParseResult {
    private final V3CliOptions options;
    private final String errorMessage;
    private final boolean helpRequested;

    private V3ParseResult(V3CliOptions options, String errorMessage, boolean helpRequested) {
        this.options = options;
        this.errorMessage = errorMessage;
        this.helpRequested = helpRequested;
    }

    public static V3ParseResult success(V3CliOptions options) {
        return new V3ParseResult(options, null, false);
    }

    public static V3ParseResult error(String message) {
        return new V3ParseResult(null, message, false);
    }

    public static V3ParseResult help() {
        return new V3ParseResult(null, null, true);
    }

    public boolean valid() { return options != null; }
    public boolean helpRequested() { return helpRequested; }
    public V3CliOptions options() { return options; }
    public String errorMessage() { return errorMessage; }
}
