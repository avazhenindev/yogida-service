package com.yogida.meditation;

import com.yogida.meditation.config.JwtProperties;
import com.yogida.meditation.config.MediaDurationProperties;
import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.config.r2.R2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({R2Properties.class, MediaDurationProperties.class, JwtProperties.class, RevenueCatProperties.class})
@SpringBootApplication
public class MeditationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeditationApplication.class, args);
    }
}
