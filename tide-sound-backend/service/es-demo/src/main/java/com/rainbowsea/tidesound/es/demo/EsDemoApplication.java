package com.rainbowsea.tidesound.es.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class EsDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(EsDemoApplication.class,args);
    }
}