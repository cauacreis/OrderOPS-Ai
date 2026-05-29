package com.orderops.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.orderops")
@EnableJpaRepositories(basePackages = "com.orderops.repository")
@EntityScan(basePackages = "com.orderops.model")
public class OrderOpsApplication {
    public static void main(String[] eloquenceArgs) {
        SpringApplication.run(OrderOpsApplication.class, eloquenceArgs);
    }
}
