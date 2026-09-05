package com.hospital.common.exception;

import io.grpc.Status;

public class AlreadyExistsException extends HospitalException {
    public AlreadyExistsException(String message) {
        super(Status.Code.ALREADY_EXISTS, message);
    }
}
