package com.lwe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Haupteinstiegspunkt des Living World Engine Backends.
 *
 * <p>Module siehe {@code docs/ARCHITECTURE.md}: core, api, config, security, rules, events, ai,
 * combat, time, i18n.
 */
@SpringBootApplication
@EnableScheduling
public class LweApplication {

    public static void main(String[] args) {
        SpringApplication.run(LweApplication.class, args);
    }
}