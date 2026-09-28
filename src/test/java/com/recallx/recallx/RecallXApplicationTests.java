package com.recallx.recallx;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Disabled;

@Disabled("Requires local Docker daemon for Testcontainers")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecallXApplicationTests {

	@Test
	void contextLoads() {
	}

}
