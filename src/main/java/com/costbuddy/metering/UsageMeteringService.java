package com.costbuddy.metering;

import com.motherboard.sdk.model.ResourceType;

public interface UsageMeteringService {

    UsageMeteringResult report(String meterItemCode, String idempotencyKey, ResourceType resourceType, String resourceId);
}
