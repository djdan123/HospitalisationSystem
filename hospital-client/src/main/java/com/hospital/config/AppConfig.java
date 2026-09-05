package com.hospital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge et expose la configuration de l'application (application.properties).
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    private AppConfig() {}

    public static synchronized void load() {
        if (loaded) return;
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                loaded = true;
                log.info("Configuration chargée depuis application.properties");
            } else {
                log.warn("application.properties introuvable, utilisation des valeurs par défaut");
            }
        } catch (IOException e) {
            log.error("Erreur de chargement de la configuration", e);
        }
    }

    public static String get(String key, String defaultValue) {
        if (!loaded) load();
        return props.getProperty(key, defaultValue);
    }

    public static String getServerHost() {
        return get("grpc.server.host", "192.168.1.10");
    }

    public static int getPort(String service) {
        return Integer.parseInt(get("grpc." + service + ".port", "50051"));
    }

    public static int getDeadlineSeconds() {
        return Integer.parseInt(get("grpc.deadline.seconds", "10"));
    }

    public static String getTheme() {
        return get("app.theme", "light");
    }

    public static String getAppTitle() {
        return get("app.title", "Hospital Management System");
    }
}
