package com.hospital.common.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepteur de logging des appels gRPC entrants.
 */
public class LoggingInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();
        log.info("gRPC call received: {}", method);

        long start = System.currentTimeMillis();
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onComplete() {
                log.info("gRPC call completed: {} ({} ms)", method, System.currentTimeMillis() - start);
                super.onComplete();
            }

            @Override
            public void onCancel() {
                log.warn("gRPC call cancelled: {}", method);
                super.onCancel();
            }
        };
    }
}
