package com.recallx.recallx;

import org.springframework.boot.SpringApplication;

public class TestRecallXApplication {

	public static void main(String[] args) {
		SpringApplication.from(RecallXApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
