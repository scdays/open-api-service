package com.vtc.openapi;

import com.botany.spore.core.utils.MessageUtil;
import com.botany.spore.liquibase.autoconfig.EnableLiquibase;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableLiquibase
@EnableFeignClients({"com.vtc.openapi"})
@SpringCloudApplication
@MapperScan("com.vtc.openapi.infra.dao")
@EnableConfigurationProperties(OpenApiProperties.class)
public class ApplicationStart {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStart.class);

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStart.class, args);
        logger.info("{} started, result: {}", ApplicationStart.class.getSimpleName(),
                MessageUtil.get("application.start.success"));
    }
}
