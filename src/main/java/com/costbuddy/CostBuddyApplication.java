package com.costbuddy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.costbuddy.mapper")
@SpringBootApplication
public class CostBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostBuddyApplication.class, args);
    }
}
