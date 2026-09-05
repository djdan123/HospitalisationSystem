package com.hospital.laboratoire.config;
import com.hospital.common.grpc.GrpcExceptionInterceptor;
import com.hospital.common.grpc.LoggingInterceptor;
import com.hospital.laboratoire.grpc.LaboratoireGrpcService;
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
    @Value("${grpc.laboratoire.port:50055}") private int port;
    @Value("${server.ip:192.168.1.10}") private String serverIp;
    private Server server;
    @Bean public CommandLineRunner start(LaboratoireGrpcService svc) {
        return args -> {
            server = ServerBuilder.forPort(port).addService(svc).intercept(new LoggingInterceptor()).intercept(new GrpcExceptionInterceptor()).build().start();
            log.info("SERVICE LABORATOIRE gRPC démarré sur {}:{}", serverIp, port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (server != null) server.shutdown(); }));
            server.awaitTermination();
        };
    }
    @PreDestroy public void stop() throws InterruptedException { if (server != null) server.shutdown().awaitTermination(5, TimeUnit.SECONDS); }
}
