package com.elmlite.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.elmlite.platform.mapper")
@SpringBootApplication
public class ElmLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElmLiteApplication.class, args);
    }
}
