package com.costbuddy.metering;

import com.motherboard.sdk.model.UsageDecision;

public record UsageMeteringResult(UsageDecision decision, String reason) {

    public boolean allowed() {
        return decision == UsageDecision.ALLOW;
    }
}
