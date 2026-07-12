package com.costbuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.motherboard.MotherboardGateway;
import com.motherboard.sdk.model.Region;
import com.motherboard.sdk.model.UserStatus;
import com.motherboard.sdk.model.request.SsoSessionRequest;
import com.motherboard.sdk.model.response.SsoSessionResponse;
import com.motherboard.sdk.model.response.User;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

class OAuthLoginServiceTest {

    private MotherboardGateway motherboardGateway;
    private CurrentUserContext currentUserContext;
    private OAuthLoginService  service;

    @BeforeEach
    void setUp() {
        AuthenticationProperties properties = new AuthenticationProperties();
        properties.getGoogle().setEnabled(true);
        properties.getGoogle().setClientId("google-client-id");
        properties.getGoogle().setRedirectUri("http://localhost:8766/api/auth/callback/GOOGLE");

        motherboardGateway = mock(MotherboardGateway.class);
        currentUserContext = new CurrentUserContext();
        service = new OAuthLoginService(properties, motherboardGateway, currentUserContext);
    }

    @Test
    void beginsGoogleLoginWithStateStoredInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        URI location = service.begin("google", request);

        var query = UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertThat(location.getHost()).isEqualTo("accounts.google.com");
        assertThat(query.getFirst("client_id")).isEqualTo("google-client-id");
        assertThat(query.getFirst("redirect_uri")).isEqualTo("http://localhost:8766/api/auth/callback/GOOGLE");
        assertThat(query.getFirst("state")).isNotBlank();
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void completesLoginAndEstablishesCurrentUserSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        URI location = service.begin("GOOGLE", request);
        String state = UriComponentsBuilder.fromUri(location).build().getQueryParams().getFirst("state");
        User user = new User(42L, Region.GLOBAL, UserStatus.ACTIVE, "Alice", "alice@example.com", null, null, null, null, null);
        when(motherboardGateway.createSsoSession(any())).thenReturn(new SsoSessionResponse(user, "motherboard-token", 3600L));

        service.complete("GOOGLE", "authorization-code", state, null, request);

        CurrentUser currentUser = currentUserContext.require(request);
        assertThat(currentUser.motherboardUserId()).isEqualTo(42L);
        assertThat(currentUser.displayName()).isEqualTo("Alice");
        assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(3600);

        ArgumentCaptor<SsoSessionRequest> requestCaptor = ArgumentCaptor.forClass(SsoSessionRequest.class);
        verify(motherboardGateway).createSsoSession(requestCaptor.capture());
        assertThat(requestCaptor.getValue().authorizationCode()).isEqualTo("authorization-code");
        assertThat(requestCaptor.getValue().redirectUri()).isEqualTo("http://localhost:8766/api/auth/callback/GOOGLE");
    }

    @Test
    void rejectsAndConsumesInvalidStateBeforeCallingMotherboard() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        service.begin("GOOGLE", request);

        assertThatThrownBy(() -> service.complete("GOOGLE", "authorization-code", "wrong-state", null, request))
            .isInstanceOfSatisfying(AuthenticationFlowException.class, exception -> assertThat(exception.getCode()).isEqualTo("OAUTH_STATE_INVALID"));
        assertThatThrownBy(() -> service.complete("GOOGLE", "authorization-code", "wrong-state", null, request)).isInstanceOf(AuthenticationFlowException.class);
        verify(motherboardGateway, never()).createSsoSession(any());
    }

}
