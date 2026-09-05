package com.hospital.consultation.config;

import com.hospital.common.grpc.GrpcExceptionInterceptor;
import com.hospital.common.grpc.LoggingInterceptor;
import com.hospital.consultation.grpc.ConsultationGrpcService;
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

    private static final Logger log =
            LoggerFactory.getLogger(GrpcServerConfig.class);

    @Value("${grpc.consultation.port:50054}")
    private int port;

    @Value("${server.ip:192.168.1.10}")
    private String serverIp;

    private Server server;

    @Bean
    public CommandLineRunner start(ConsultationGrpcService svc) {
        return args -> {

            // Démarrage du serveur gRPC
            server = ServerBuilder.forPort(port)
                    .addService(svc)
                    .intercept(new LoggingInterceptor())
                    .intercept(new GrpcExceptionInterceptor())
                    .build()
                    .start();

            // Message de démarrage
            log.info("=================================================");
            log.info("  SERVICE CONSULTATION gRPC démarré");
            log.info("  IP     : {}", serverIp);
            log.info("  Port   : {}", port);
            log.info("=================================================");

            // Arrêt propre lorsque l'application se ferme
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Arrêt du service Consultation gRPC...");

                if (server != null) {
                    server.shutdown();
                }
            }));

            // IMPORTANT :
            // Maintient le serveur gRPC actif
            server.awaitTermination();
        };
    }

    /**
     * Arrêt propre du serveur lors de la destruction
     * du contexte Spring.
     */
    @PreDestroy
    public void stop() throws InterruptedException {

        if (server != null) {
            server.shutdown()
                    .awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}