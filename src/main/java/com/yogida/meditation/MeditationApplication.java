package com.yogida.meditation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MeditationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeditationApplication.class, args);
    }
}
