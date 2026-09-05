package com.hospital.common.exception;

import io.grpc.Status;

public class NotFoundException extends HospitalException {
    public NotFoundException(String message) {
        super(Status.Code.NOT_FOUND, message);
    }
}
