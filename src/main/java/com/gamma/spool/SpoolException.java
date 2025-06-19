package com.gamma.spool;

public class SpoolException extends RuntimeException {

    public SpoolException() {
        super("Spool has failed for an unspecified reason.");
    }

    public SpoolException(String message) {
        super(message);
    }
}
