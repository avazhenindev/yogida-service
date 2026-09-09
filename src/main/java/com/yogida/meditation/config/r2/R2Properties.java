package com.yogida.meditation.config.r2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record R2Properties(
    String accountId,
    String accessKeyId,
    String secretAccessKey,
    String bucket
) {

    /**
     * The S3 API endpoint for this account.
     *
     * <p>Assembled in three places before this: the S3Client, the S3Presigner, and the
     * {@code base_url} persisted on every media object row. Moving to a jurisdiction-specific
     * host, a custom domain or a local MinIO stand-in meant three coordinated edits, and getting
     * two of the three desynchronises the signer from the client or records a host that no longer
     * resolves.
     *
     * <p>An extra accessor is safe on a {@code @ConfigurationProperties} record: Spring binds
     * through the canonical constructor and ignores methods that are not components.
     */
    public String s3Endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }
}
