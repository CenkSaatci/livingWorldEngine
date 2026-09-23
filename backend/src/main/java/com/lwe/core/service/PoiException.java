package com.lwe.core.service;

/** Fachlicher Fehler einer POI-Aktion (ADR-015) mit stabilem Fehlercode. */
public class PoiException extends RuntimeException {

    private final String errorCode;

    public PoiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
