package com.costbuddy.auth;

import com.motherboard.sdk.model.IdentityProvider;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cost-buddy.authentication")
public class AuthenticationProperties {

    private String successRedirect = "/";
    private String failureRedirect = "/";
    private Google google          = new Google();
    private Wechat wechat          = new Wechat();

    public List<IdentityProvider> configuredProviders() {
        List<IdentityProvider> providers = new ArrayList<>();
        if (google.isConfigured()) {
            providers.add(IdentityProvider.GOOGLE);
        }
        if (wechat.isConfigured()) {
            providers.add(IdentityProvider.WECHAT);
        }
        return providers;
    }

    @Data
    public static class Google {

        private boolean enabled;
        private String  clientId         = "";
        private String  redirectUri      = "";
        private String  authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";

        public boolean isConfigured() { return enabled && hasText(clientId) && hasText(redirectUri) && hasText(authorizationUri); }
    }

    @Data
    public static class Wechat {

        private boolean enabled;
        private String  appId            = "";
        private String  redirectUri      = "";
        private String  authorizationUri = "https://open.weixin.qq.com/connect/qrconnect";

        public boolean isConfigured() { return enabled && hasText(appId) && hasText(redirectUri) && hasText(authorizationUri); }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
