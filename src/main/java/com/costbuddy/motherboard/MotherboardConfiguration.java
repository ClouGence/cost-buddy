package com.costbuddy.motherboard;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.motherboard.sdk.MotherboardClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MotherboardProperties.class)
public class MotherboardConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
    public MotherboardClient motherboardClient(MotherboardProperties properties) {
        validate(properties);
        return MotherboardClient.builder()
            .baseUrl(properties.getBaseUrl())
            .productId(properties.getProductId())
            .privateKey(properties.getPrivateKey())
            .requestTimeout(properties.getTimeout())
            .build();
    }

    private void validate(MotherboardProperties properties) {
        requireText(properties.getBaseUrl(), "motherboard.base-url");
        if (properties.getProductId() <= 0) {
            throw new IllegalStateException("motherboard.product-id must be greater than zero when Motherboard is enabled");
        }
        requireText(properties.getPrivateKey(), "motherboard.private-key");
        Duration timeout = properties.getTimeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("motherboard.timeout must be positive when Motherboard is enabled");
        }
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured when Motherboard is enabled");
        }
    }
}
