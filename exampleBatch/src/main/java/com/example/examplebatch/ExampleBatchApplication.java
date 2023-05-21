package com.example.examplebatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class ExampleBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExampleBatchApplication.class, args);
    }

}
