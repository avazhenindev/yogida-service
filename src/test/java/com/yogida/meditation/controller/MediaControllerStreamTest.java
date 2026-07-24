package com.yogida.meditation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogida.meditation.advice.GlobalExceptionHandler;
import com.yogida.meditation.config.TestSecurityConfig;
import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.service.SecureStreamService;
import com.yogida.meditation.service.UserMediaFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link MediaController} focusing on the stream endpoint
 * and entitlement error handling via {@link GlobalExceptionHandler}.
 */
@ExtendWith(MockitoExtension.class)
class MediaControllerStreamTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SecureStreamService secureStreamService;

    @Mock
    private UserMediaFacadeService userMediaFacadeService;

    @InjectMocks
    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void getStreamUrl_whenUserIsNotEntitled_returns403WithMessage() throws Exception {
        when(secureStreamService.generateSecureStreamingUrl(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not entitled to access this media"));

        S3ObjectDto dto = new S3ObjectDto();
        dto.setBucketName("test-bucket");
        dto.setObjectUri("media/test.mp3");

        mockMvc.perform(post("/media/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Not entitled to access this media"))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void getStreamUrl_whenMediaNotFound_returns404() throws Exception {
        when(secureStreamService.generateSecureStreamingUrl(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));

        S3ObjectDto dto = new S3ObjectDto();
        dto.setBucketName("test-bucket");
        dto.setObjectUri("media/missing.mp3");

        mockMvc.perform(post("/media/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Media not found"));
    }

    @Test
    void getStreamUrl_whenEntitled_returns200WithUrl() throws Exception {
        when(secureStreamService.generateSecureStreamingUrl(any()))
            .thenReturn(java.util.Map.of("url", "https://r2.example.com/bucket/media.mp3?signed=1"));

        S3ObjectDto dto = new S3ObjectDto();
        dto.setBucketName("test-bucket");
        dto.setObjectUri("media/test.mp3");

        mockMvc.perform(post("/media/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("https://r2.example.com/bucket/media.mp3?signed=1"));
    }
}
