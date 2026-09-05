package com.hospital.common.exception;

import io.grpc.Status;

public class InvalidArgumentException extends HospitalException {
    public InvalidArgumentException(String message) {
        super(Status.Code.INVALID_ARGUMENT, message);
    }
}
