package com.hospital.hospitalisation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.hospitalisation", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.hospitalisation.entity")
@EnableJpaRepositories(basePackages = "com.hospital.hospitalisation.repository")
public class HospitalisationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HospitalisationServiceApplication.class, args);
    }
}
