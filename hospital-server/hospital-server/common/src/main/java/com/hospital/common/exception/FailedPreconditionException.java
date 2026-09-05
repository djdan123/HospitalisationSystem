package com.hospital.common.exception;

import io.grpc.Status;

public class FailedPreconditionException extends HospitalException {
    public FailedPreconditionException(String message) {
        super(Status.Code.FAILED_PRECONDITION, message);
    }
}
