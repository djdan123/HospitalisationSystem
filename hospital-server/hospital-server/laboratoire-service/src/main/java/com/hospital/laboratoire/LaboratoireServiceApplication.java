package com.hospital.laboratoire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.laboratoire", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.laboratoire.entity")
@EnableJpaRepositories(basePackages = "com.hospital.laboratoire.repository")
public class LaboratoireServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaboratoireServiceApplication.class, args);
    }
}
