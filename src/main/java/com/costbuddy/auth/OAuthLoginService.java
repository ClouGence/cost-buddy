package com.costbuddy.auth;

import com.costbuddy.motherboard.MotherboardGateway;
import com.motherboard.sdk.model.IdentityProvider;
import com.motherboard.sdk.model.Region;
import com.motherboard.sdk.model.request.SsoSessionRequest;
import com.motherboard.sdk.model.response.SsoSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class OAuthLoginService {

    private static final String            OAUTH_STATE_ATTRIBUTE = "costbuddy.oauthState";
    private static final Duration          OAUTH_STATE_TTL       = Duration.ofMinutes(10);

    private final AuthenticationProperties properties;
    private final MotherboardGateway       motherboardGateway;
    private final CurrentUserContext       currentUserContext;
    private final SecureRandom             secureRandom          = new SecureRandom();

    public OAuthLoginService(AuthenticationProperties properties, MotherboardGateway motherboardGateway, CurrentUserContext currentUserContext){
        this.properties = properties;
        this.motherboardGateway = motherboardGateway;
        this.currentUserContext = currentUserContext;
    }

    public URI begin(String providerValue, HttpServletRequest request) {
        IdentityProvider provider = parseProvider(providerValue);
        ProviderConfiguration providerConfiguration = providerConfiguration(provider);
        String state = generateState();
        request.getSession(true).setAttribute(OAUTH_STATE_ATTRIBUTE, new OAuthState(state, provider, providerConfiguration.redirectUri(), Instant.now()));
        return authorizationUri(provider, providerConfiguration, state);
    }

    public void complete(String providerValue, String authorizationCode, String state, String providerError, HttpServletRequest request) {
        IdentityProvider provider = parseProvider(providerValue);
        OAuthState oauthState = consumeAndValidateState(provider, state, request);
        if (hasText(providerError)) {
            throw new AuthenticationFlowException("OAUTH_PROVIDER_REJECTED", "OAuth provider rejected the login request");
        }
        if (!hasText(authorizationCode)) {
            throw new AuthenticationFlowException("OAUTH_CODE_MISSING", "OAuth authorization code is missing");
        }

        Region region = provider == IdentityProvider.WECHAT ? Region.CN : Region.GLOBAL;
        SsoSessionResponse response = motherboardGateway.createSsoSession(new SsoSessionRequest(provider, authorizationCode, region, oauthState.redirectUri()));
        validateResponse(response);
        CurrentUser currentUser = new CurrentUser(response.user().id(), response.user().displayName(), response.user().email());
        currentUserContext.establish(request, currentUser, response.token(), response.expiresIn());
    }

    public URI successRedirect() {
        return redirectUri(properties.getSuccessRedirect(), null);
    }

    public URI failureRedirect(String code) {
        return redirectUri(properties.getFailureRedirect(), code);
    }

    private OAuthState consumeAndValidateState(IdentityProvider provider, String state, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw invalidState();
        }
        Object value = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        if (!(value instanceof OAuthState oauthState)) {
            throw invalidState();
        }
        boolean expired = oauthState.createdAt().plus(OAUTH_STATE_TTL).isBefore(Instant.now());
        if (expired || oauthState.provider() != provider || !secureEquals(oauthState.value(), state)) {
            throw invalidState();
        }
        return oauthState;
    }

    private URI authorizationUri(IdentityProvider provider, ProviderConfiguration configuration, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(configuration.authorizationUri());
        if (provider == IdentityProvider.GOOGLE) {
            return builder.queryParam("client_id", configuration.clientId())
                .queryParam("redirect_uri", configuration.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
        }
        return builder.queryParam("appid", configuration.clientId())
            .queryParam("redirect_uri", configuration.redirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", "snsapi_login")
            .queryParam("state", state)
            .fragment("wechat_redirect")
            .build()
            .encode()
            .toUri();
    }

    private ProviderConfiguration providerConfiguration(IdentityProvider provider) {
        if (provider == IdentityProvider.GOOGLE && properties.getGoogle().isConfigured()) {
            AuthenticationProperties.Google google = properties.getGoogle();
            return new ProviderConfiguration(google.getClientId(), google.getRedirectUri(), google.getAuthorizationUri());
        }
        if (provider == IdentityProvider.WECHAT && properties.getWechat().isConfigured()) {
            AuthenticationProperties.Wechat wechat = properties.getWechat();
            return new ProviderConfiguration(wechat.getAppId(), wechat.getRedirectUri(), wechat.getAuthorizationUri());
        }
        throw new AuthenticationFlowException("OAUTH_PROVIDER_NOT_CONFIGURED", "OAuth provider is not configured: " + provider);
    }

    private IdentityProvider parseProvider(String value) {
        try {
            return IdentityProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AuthenticationFlowException("OAUTH_PROVIDER_UNSUPPORTED", "Unsupported OAuth provider");
        }
    }

    private void validateResponse(SsoSessionResponse response) {
        if (response == null || response.user() == null || response.user().id() == null || !hasText(response.token())) {
            throw new AuthenticationFlowException("MOTHERBOARD_SESSION_INVALID", "Motherboard returned an invalid user session");
        }
    }

    private String generateState() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private AuthenticationFlowException invalidState() {
        return new AuthenticationFlowException("OAUTH_STATE_INVALID", "OAuth state is missing, expired, or invalid");
    }

    private URI redirectUri(String target, String errorCode) {
        if (!hasText(target)) {
            throw new IllegalStateException("authentication redirect target must not be blank");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(target);
        if (hasText(errorCode)) {
            builder.queryParam("authError", errorCode);
        }
        return builder.build().encode().toUri();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ProviderConfiguration(String clientId, String redirectUri, String authorizationUri) {
    }

    private record OAuthState(String value, IdentityProvider provider, String redirectUri, Instant createdAt) implements Serializable {
    }
}
