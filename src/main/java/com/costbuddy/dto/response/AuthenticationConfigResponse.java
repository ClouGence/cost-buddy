package com.costbuddy.dto.response;

import com.motherboard.sdk.model.IdentityProvider;
import java.util.List;

public record AuthenticationConfigResponse(boolean enabled, List<IdentityProvider> providers) {
}
