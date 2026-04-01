package com.yogida.meditation.config.r2;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class R2ClientConfig {

    @Bean
    public S3Client s3Client(R2Properties props) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            props.accessKeyId(),
            props.secretAccessKey()
        );

        return S3Client.builder()
            .endpointOverride(URI.create("https://" + props.accountId() + ".r2.cloudflarestorage.com"))
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build())
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(R2Properties props) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            props.accessKeyId(),
            props.secretAccessKey()
        );

        return S3Presigner.builder()
            .endpointOverride(URI.create("https://" + props.accountId() + ".r2.cloudflarestorage.com"))
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();
    }
}
