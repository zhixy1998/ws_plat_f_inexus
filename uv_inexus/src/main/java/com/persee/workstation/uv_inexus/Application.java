package com.persee.workstation.uv_inexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.persee.workstation.uv_inexus")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}