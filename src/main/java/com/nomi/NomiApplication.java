package com.nomi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class NomiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NomiApplication.class, args);
    }
}
