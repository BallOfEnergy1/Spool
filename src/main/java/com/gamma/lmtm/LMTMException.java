package com.gamma.lmtm;

public class LMTMException extends RuntimeException {

    public LMTMException() {
        super("LMTM has failed for an unspecified reason.");
    }

    public LMTMException(String message) {
        super(message);
    }
}
