package com.vtc.openapi.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "open-api.task-center.kafka", name = "enabled", havingValue = "true")
public class TaskCenterKafkaConfig {
}
