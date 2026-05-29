package com.orderops.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.orderops")
public class OrderOpsApplication {
    public static void main(String[] eloquenceArgs) {
        SpringApplication.run(OrderOpsApplication.class, eloquenceArgs);
    }
}
