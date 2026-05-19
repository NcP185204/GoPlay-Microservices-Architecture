package com.caophuc.court;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // Đăng ký với Eureka
public class CourtApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourtApplication.class, args);
    }

}
