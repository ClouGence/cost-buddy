package com.costbuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.costbuddy.motherboard.MotherboardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CurrentUserProviderTest {

    @Test
    void usesReservedLocalUserWhenMotherboardIsDisabled() {
        MotherboardProperties properties = new MotherboardProperties();
        CurrentUserProvider provider = new CurrentUserProvider(properties, mock(CurrentUserContext.class), mock(MockHttpServletRequest.class));

        assertThat(provider.motherboardUserId()).isEqualTo(CurrentUserProvider.LOCAL_USER_ID);
    }

    @Test
    void usesAuthenticatedMotherboardUser() {
        MotherboardProperties properties = new MotherboardProperties();
        properties.setEnabled(true);
        CurrentUserContext context = new CurrentUserContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        context.establish(request, new CurrentUser(42L, "Alice", "alice@example.com"), "token", 3600L);
        CurrentUserProvider provider = new CurrentUserProvider(properties, context, request);

        assertThat(provider.motherboardUserId()).isEqualTo(42L);
    }
}
