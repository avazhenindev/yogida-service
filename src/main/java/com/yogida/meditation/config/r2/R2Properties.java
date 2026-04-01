package com.yogida.meditation.config.r2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record R2Properties(
    String accountId,
    String accessKeyId,
    String secretAccessKey,
    String bucket
) { }
