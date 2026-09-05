package com.hospital.paiement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.paiement", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.paiement.entity")
@EnableJpaRepositories(basePackages = "com.hospital.paiement.repository")
public class PaiementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaiementServiceApplication.class, args);
    }
}
