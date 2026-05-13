package com.smg.sampleconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SampleConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleConsumerApplication.class, args);
    }
}
