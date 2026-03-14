package com.officearcade.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OfficeArcadeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficeArcadeServerApplication.class, args);
    }
}
