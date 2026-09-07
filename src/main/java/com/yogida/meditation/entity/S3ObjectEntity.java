package com.yogida.meditation.entity;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
// Class-level @BatchSize applies to lazy proxies OF this type, so wherever a collection of
// rows each holds an s3_object reference, Hibernate initialises them as one
// "... WHERE id IN (?,?,...)" instead of one select apiece. This is what keeps breathing
// phase audio from costing a query per file.
@BatchSize(size = 200)
@Table(name = "s3_object")
public class S3ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "base_url", nullable = false, length = 2048)
    private String baseUrl;

    @Column(name = "object_uri", nullable = false, length = 2048)
    private String objectUri;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    public String getFullUrl() {
        String normalizedBaseUrl = baseUrl;
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/" + objectUri;
    }
}

