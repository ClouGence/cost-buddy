package com.costbuddy.subscription;

import com.costbuddy.dto.response.CurrentSubscriptionResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCurrentSubscriptionService implements CurrentSubscriptionService {

    @Override
    public CurrentSubscriptionResponse getCurrent() { return new CurrentSubscriptionResponse(false, null, null, "Local", "LOCAL", null, false); }
}
