package com.costbuddy.metering;

import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.UsageDecision;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalUsageMeteringService implements UsageMeteringService {

    @Override
    public UsageMeteringResult report(String meterItemCode, String idempotencyKey, ResourceType resourceType, String resourceId) {
        return new UsageMeteringResult(UsageDecision.ALLOW, "MOTHERBOARD_DISABLED");
    }
}
