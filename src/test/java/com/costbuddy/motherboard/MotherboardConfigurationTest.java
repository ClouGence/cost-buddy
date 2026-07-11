package com.costbuddy.motherboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motherboard.sdk.MotherboardClient;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MotherboardConfigurationTest {

    @Test
    void createsReusableClientFromProperties() throws Exception {
        MotherboardProperties properties = validProperties();

        MotherboardClient client = new MotherboardConfiguration().motherboardClient(properties);

        assertThat(client).isNotNull();
    }

    @Test
    void rejectsIncompleteEnabledConfiguration() {
        MotherboardProperties properties = new MotherboardProperties();
        properties.setEnabled(true);

        assertThatThrownBy(() -> new MotherboardConfiguration().motherboardClient(properties)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("motherboard.base-url");
    }

    private MotherboardProperties validProperties() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        MotherboardProperties properties = new MotherboardProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:8966");
        properties.setProductId(1L);
        properties.setPrivateKey(Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()));
        properties.setTimeout(Duration.ofSeconds(5));
        return properties;
    }
}
