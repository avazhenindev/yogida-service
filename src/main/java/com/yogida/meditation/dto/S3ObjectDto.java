package com.yogida.meditation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectDto {
    private Long id;
    private String bucketName;
    private String baseUrl;
    private String objectUri;
    private String url;
}

