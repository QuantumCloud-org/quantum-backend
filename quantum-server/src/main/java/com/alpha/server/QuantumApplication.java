package com.alpha.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.InetAddress;

@Slf4j
@MapperScan("com.alpha.**.mapper")
@EnableCaching
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.alpha")
public class QuantumApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(QuantumApplication.class, args);
        printStartupInfo(context);
    }

    /**
     * 打印启动信息
     */
    private static void printStartupInfo(ConfigurableApplicationContext context) {
        try {
            Environment env = context.getEnvironment();
            String ip = InetAddress.getLocalHost().getHostAddress();
            String port = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String appName = env.getProperty("spring.application.name", "Quantum");

            log.info("""
                            
                            ----------------------------------------------------------
                            \tApplication '{}' is running! Access URLs:
                            \tLocal:    http://localhost:{}{}
                            \tExternal: http://{}:{}{}
                            \tDoc:      http://{}:{}{}/doc.html
                            ----------------------------------------------------------
                            """,
                    appName, port, contextPath, ip, port, contextPath, ip, port, contextPath);
        } catch (Exception e) {
            log.warn("获取启动信息失败", e);
        }
    }
}