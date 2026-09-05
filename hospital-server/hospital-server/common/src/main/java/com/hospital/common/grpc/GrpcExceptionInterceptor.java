package com.hospital.common.grpc;

import com.hospital.common.exception.HospitalException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepteur centralisé de gestion des exceptions gRPC.
 * Convertit les exceptions métier en Status gRPC appropriés.
 */
public class GrpcExceptionInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (HospitalException e) {
                    log.warn("Business error on {}: {} - {}", call.getMethodDescriptor().getFullMethodName(),
                            e.getStatusCode(), e.getMessage());
                    call.close(e.toStatus(), new Metadata());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid argument on {}: {}", call.getMethodDescriptor().getFullMethodName(), e.getMessage());
                    call.close(Status.INVALID_ARGUMENT.withDescription(e.getMessage()), new Metadata());
                } catch (Exception e) {
                    log.error("Unexpected error on {}", call.getMethodDescriptor().getFullMethodName(), e);
                    call.close(Status.INTERNAL.withDescription("Erreur interne du serveur").withCause(e), new Metadata());
                }
            }
        };
    }
}
