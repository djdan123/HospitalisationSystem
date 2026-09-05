package com.hospital.maternite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hospital.maternite", "com.hospital.common"})
@EntityScan(basePackages = "com.hospital.maternite.entity")
@EnableJpaRepositories(basePackages = "com.hospital.maternite.repository")
public class MaterniteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaterniteServiceApplication.class, args);
    }
}
