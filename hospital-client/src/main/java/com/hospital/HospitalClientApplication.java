package com.hospital;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée de l'application cliente Hospital Management System.
 */
public class HospitalClientApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(HospitalClientApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppConfig.load();
        log.info("Démarrage de {} v{}", AppConfig.getAppTitle(), AppConfig.get("app.version", "1.0.0"));
        log.info("Serveur cible : {}:{}", AppConfig.getServerHost(), AppConfig.getPort("accueil"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle(AppConfig.getAppTitle());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            log.info("Fermeture de l'application...");
            GrpcConfig.shutdownAll();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
