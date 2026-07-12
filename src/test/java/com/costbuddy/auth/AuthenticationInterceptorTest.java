package com.costbuddy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationInterceptorTest {

    private final CurrentUserContext        currentUserContext = new CurrentUserContext();
    private final AuthenticationInterceptor interceptor        = new AuthenticationInterceptor(currentUserContext);

    @Test
    void rejectsRequestWithoutCurrentUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    void allowsRequestWithCurrentUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        currentUserContext.establish(request, new CurrentUser(42L, "Alice", "alice@example.com"), "token", 3600L);

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void rejectsSessionAfterMotherboardTokenExpires() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        currentUserContext.establish(request, new CurrentUser(42L, "Alice", "alice@example.com"), "token", 3600L);
        request.getSession(false).setAttribute("costbuddy.sessionExpiresAt", Instant.EPOCH);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isInstanceOf(AuthenticationRequiredException.class);
    }
}
