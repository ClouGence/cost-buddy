package com.costbuddy.metering;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.request.UsageEventRequest;
import com.motherboard.sdk.model.response.UsageReportResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class MotherboardUsageMeteringService implements UsageMeteringService {

    private static final long           QUANTITY = 1L;

    private final MotherboardGateway    motherboardGateway;
    private final MotherboardProperties motherboardProperties;
    private final CurrentUserProvider   currentUserProvider;

    public MotherboardUsageMeteringService(MotherboardGateway motherboardGateway, MotherboardProperties motherboardProperties, CurrentUserProvider currentUserProvider){
        this.motherboardGateway = motherboardGateway;
        this.motherboardProperties = motherboardProperties;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public UsageMeteringResult report(String meterItemCode, String idempotencyKey, ResourceType resourceType, String resourceId) {
        String normalizedIdempotencyKey = idempotencyKey.trim();
        UsageEventRequest request = new UsageEventRequest(eventId(normalizedIdempotencyKey),
            normalizedIdempotencyKey,
            motherboardProperties.getProductId(),
            currentUserProvider.motherboardUserId(),
            meterItemCode,
            QUANTITY,
            resourceType,
            resourceId,
            null);
        UsageReportResponse response = motherboardGateway.reportUsage(request);
        if (response == null || response.decision() == null) {
            throw new IllegalStateException("Motherboard returned an invalid usage report");
        }
        return new UsageMeteringResult(response.decision(), response.reason());
    }

    private String eventId(String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return "costbuddy-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
