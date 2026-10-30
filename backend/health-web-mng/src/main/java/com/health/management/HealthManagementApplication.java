package com.health.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.health.management.mapper")
public class HealthManagementApplication {

    public static void main(String[] args) {
        // 图片水印使用 AWT/ImageIO，服务器环境需要开启 headless
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(HealthManagementApplication.class, args);
    }
}
