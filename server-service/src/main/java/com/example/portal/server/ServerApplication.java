package com.example.portal.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 认证服务入口。 */
@SpringBootApplication(scanBasePackages = {"com.example.portal.server", "com.example.portal.common"})
@MapperScan("com.example.portal.server.mapper")
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
