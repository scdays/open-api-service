package com.vtc.openapi.infra.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;

import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 开放平台 Swagger/Knife4j 配置（对齐 clover · spore {@code Swagger2Config}）。
 */
@Configuration
@EnableSwagger2
@EnableKnife4j
@Import(BeanValidatorPluginsConfiguration.class)
@ConditionalOnWebApplication
public class OpenApiSwaggerConfig {

    @Autowired
    private Environment env;

    @Bean
    public Docket openApiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .pathMapping("/")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        String appName = env.getProperty("spring.application.name", "open-api-service");
        String version = env.getProperty("info.release.version", "1.0.0");
        return new ApiInfoBuilder()
                .title(appName + " · 开放平台 RESTful API")
                .description("Partner Token / 内部管理 / 开放平台业务 API。文档 UI：/doc.html")
                .version(version)
                .build();
    }
}
