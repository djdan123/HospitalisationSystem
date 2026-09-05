package com.hospital.paiement.config;

import com.hospital.common.grpc.GrpcExceptionInterceptor;
import com.hospital.common.grpc.LoggingInterceptor;
import com.hospital.paiement.grpc.PaiementGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcServerConfig {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);
    @Value("${grpc.paiement.port:50053}") private int port;
    @Value("${server.ip:192.168.1.10}") private String serverIp;
    private Server server;

    @Bean
    public CommandLineRunner startPaiementGrpcServer(PaiementGrpcService grpcService) {
        return args -> {
            server = ServerBuilder.forPort(port)
                    .addService(grpcService)
                    .intercept(new LoggingInterceptor())
                    .intercept(new GrpcExceptionInterceptor())
                    .build().start();
            log.info("SERVICE PAIEMENT gRPC démarré sur {}:{}", serverIp, port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (server != null) server.shutdown(); }));
            server.awaitTermination();
        };
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (server != null) server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
