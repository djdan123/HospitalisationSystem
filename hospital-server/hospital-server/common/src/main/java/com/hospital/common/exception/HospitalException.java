package com.hospital.common.exception;

import io.grpc.Status;

public class HospitalException extends RuntimeException {

    private final Status.Code statusCode;

    public HospitalException(Status.Code statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HospitalException(Status.Code statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Status.Code getStatusCode() {
        return statusCode;
    }

    public Status toStatus() {
        return Status.fromCode(statusCode).withDescription(getMessage()).withCause(getCause());
    }
}
