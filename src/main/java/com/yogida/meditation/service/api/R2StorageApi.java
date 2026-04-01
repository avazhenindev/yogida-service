package com.yogida.meditation.service.api;

import java.util.List;
import java.util.Map;

public interface R2StorageApi {

    String generateStreamingUrl(String bucketName, String mediaName);

    Map<String, List<String>> getAllBucketsWithObjects();

}
