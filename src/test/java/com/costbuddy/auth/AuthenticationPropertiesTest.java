package com.costbuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.motherboard.sdk.model.IdentityProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthenticationPropertiesTest {

    @Test
    void exposesOnlyFullyConfiguredProviders() {
        AuthenticationProperties properties = new AuthenticationProperties();
        properties.getGoogle().setEnabled(true);

        assertThat(properties.configuredProviders()).isEmpty();

        properties.getGoogle().setClientId("google-client-id");
        properties.getGoogle().setRedirectUri("http://localhost:8766/api/auth/callback/GOOGLE");

        assertThat(properties.configuredProviders()).isEqualTo(List.of(IdentityProvider.GOOGLE));
    }
}
