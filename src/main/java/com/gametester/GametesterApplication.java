package com.gametester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GametesterApplication {
    public static void main(String[] args) {
        SpringApplication.run(GametesterApplication.class, args);
    }
}