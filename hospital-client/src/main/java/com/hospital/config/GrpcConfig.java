package com.hospital.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gestion centralisée des canaux gRPC vers les microservices.
 */
public final class GrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcConfig.class);
    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    private GrpcConfig() {}

    public static ManagedChannel getChannel(String serviceName, int port) {
        return channels.computeIfAbsent(serviceName, key -> {
            String host = AppConfig.getServerHost();
            log.info("Création du canal gRPC {} → {}:{}", serviceName, host, port);
            return ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()          // plaintext pour tests LAN (TLS possible ensuite)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build();
        });
    }

    public static ManagedChannel getAccueilChannel() {
        return getChannel("accueil", AppConfig.getPort("accueil"));
    }

    public static ManagedChannel getHospitalisationChannel() {
        return getChannel("hospitalisation", AppConfig.getPort("hospitalisation"));
    }

    public static ManagedChannel getPaiementChannel() {
        return getChannel("paiement", AppConfig.getPort("paiement"));
    }

    public static ManagedChannel getConsultationChannel() {
        return getChannel("consultation", AppConfig.getPort("consultation"));
    }

    public static ManagedChannel getLaboratoireChannel() {
        return getChannel("laboratoire", AppConfig.getPort("laboratoire"));
    }

    public static ManagedChannel getPharmacieChannel() {
        return getChannel("pharmacie", AppConfig.getPort("pharmacie"));
    }

    public static ManagedChannel getMaterniteChannel() {
        return getChannel("maternite", AppConfig.getPort("maternite"));
    }

    public static void shutdownAll() {
        channels.forEach((name, channel) -> {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("Canal gRPC {} fermé", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
            }
        });
        channels.clear();
    }

    public static boolean isServerReachable(String serviceName, int port) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder
                    .forAddress(AppConfig.getServerHost(), port)
                    .usePlaintext()
                    .build();
            // Simple connectivity check
            channel.getState(true);
            return true;
        } catch (Exception e) {
            log.debug("Serveur {} non joignable: {}", serviceName, e.getMessage());
            return false;
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }
}
