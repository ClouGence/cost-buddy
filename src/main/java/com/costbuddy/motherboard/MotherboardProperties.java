package com.costbuddy.motherboard;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "motherboard")
public class MotherboardProperties {

    private boolean  enabled;
    private String   baseUrl    = "";
    private long     productId;
    private String   privateKey = "";
    private Duration timeout    = Duration.ofSeconds(30);
}
