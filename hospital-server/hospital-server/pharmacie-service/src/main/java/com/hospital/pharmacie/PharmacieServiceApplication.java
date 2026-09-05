package com.hospital.pharmacie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.pharmacie", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.pharmacie.entity")
@EnableJpaRepositories(basePackages = "com.hospital.pharmacie.repository")
public class PharmacieServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PharmacieServiceApplication.class, args);
    }
}
