package com.example.portal.console;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 管理后台服务入口。 */
@SpringBootApplication(scanBasePackages = {"com.example.portal.console", "com.example.portal.common"})
@EnableFeignClients
@MapperScan("com.example.portal.console.mapper")
public class ConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }
}
