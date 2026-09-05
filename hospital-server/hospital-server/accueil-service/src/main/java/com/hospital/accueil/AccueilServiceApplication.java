package com.hospital.accueil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.accueil", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.accueil.entity")
@EnableJpaRepositories(basePackages = "com.hospital.accueil.repository")
public class AccueilServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccueilServiceApplication.class, args);
    }
}
