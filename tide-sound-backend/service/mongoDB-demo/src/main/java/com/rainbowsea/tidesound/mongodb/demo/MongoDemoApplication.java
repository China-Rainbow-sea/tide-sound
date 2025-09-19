package com.rainbowsea.tidesound.mongodb.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 *
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MongoDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MongoDemoApplication.class, args);
    }
}