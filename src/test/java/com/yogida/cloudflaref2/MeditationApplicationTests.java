package com.yogida.cloudflaref2;

import com.yogida.meditation.MeditationApplication;
import com.yogida.meditation.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MeditationApplication.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class MeditationApplicationTests {

    @Test
    void contextLoads() {
    }

}
