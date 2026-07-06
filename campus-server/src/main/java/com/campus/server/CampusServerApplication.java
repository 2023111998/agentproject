package com.campus.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.campus.server.client")
public class CampusServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusServerApplication.class, args);
    }
}
