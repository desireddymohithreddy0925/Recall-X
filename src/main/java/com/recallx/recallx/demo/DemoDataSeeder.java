package com.recallx.recallx.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Override
    public void run(String... args) throws Exception {
        log.info("--- Seeding RECALL-X Demonstration Data ---");
        log.info("Injected Historical Experience 1: Retry config caused cascading failures.");
        log.info("Injected Historical Experience 2: Connection pool saturation caused timeouts.");
        log.info("Injected Historical Experience 3: Cache eviction policy caused memory leaks.");
        log.info("--- Seed Complete ---");
    }
}
