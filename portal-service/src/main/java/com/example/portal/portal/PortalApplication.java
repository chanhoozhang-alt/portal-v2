package com.example.portal.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 门户前端服务入口。 */
@SpringBootApplication(
    scanBasePackages = {"com.example.portal.portal", "com.example.portal.common"},
    exclude = {DataSourceAutoConfiguration.class}
)
@EnableFeignClients
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }
}
