package com.costbuddy.metering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.UsageDecision;
import com.motherboard.sdk.model.request.UsageEventRequest;
import com.motherboard.sdk.model.response.UsageReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MotherboardUsageMeteringServiceTest {

    private static final long               USER_ID     = 42L;
    private static final long               PRODUCT_ID  = 7L;
    private static final String             RESOURCE_ID = "aliyun-credential:test";

    private MotherboardGateway              gateway;
    private MotherboardUsageMeteringService service;

    @BeforeEach
    void setUp() {
        gateway = mock(MotherboardGateway.class);
        MotherboardProperties properties = new MotherboardProperties();
        properties.setProductId(PRODUCT_ID);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.motherboardUserId()).thenReturn(USER_ID);
        service = new MotherboardUsageMeteringService(gateway, properties, currentUserProvider);
    }

    @Test
    void reportsUsageWithExpectedMeterAndCredentialResource() {
        when(gateway.reportUsage(any())).thenReturn(new UsageReportResponse(1L, "event", "retry-key", UsageDecision.ALLOW, "RECORDED", false));

        UsageMeteringResult result = service.report(MeterItemCodes.AUDIT_RUN, " retry-key ", ResourceType.ALIYUN_CREDENTIAL, RESOURCE_ID);

        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isEqualTo("RECORDED");
        ArgumentCaptor<UsageEventRequest> captor = ArgumentCaptor.forClass(UsageEventRequest.class);
        verify(gateway).reportUsage(captor.capture());
        UsageEventRequest request = captor.getValue();
        assertThat(request.idempotencyKey()).isEqualTo("retry-key");
        assertThat(request.productId()).isEqualTo(PRODUCT_ID);
        assertThat(request.userId()).isEqualTo(USER_ID);
        assertThat(request.meterItemCode()).isEqualTo(MeterItemCodes.AUDIT_RUN);
        assertThat(request.quantity()).isEqualTo(1L);
        assertThat(request.resourceType()).isEqualTo(ResourceType.ALIYUN_CREDENTIAL);
        assertThat(request.resourceId()).isEqualTo(RESOURCE_ID);
    }

    @Test
    void reusesEventAndIdempotencyKeysWhenCallerRetries() {
        when(gateway.reportUsage(any())).thenReturn(new UsageReportResponse(1L, "event", "retry-key", UsageDecision.REJECT, "QUOTA_EXCEEDED", false),
            new UsageReportResponse(1L, "event", "retry-key", UsageDecision.REJECT, "QUOTA_EXCEEDED", true));

        service.report(MeterItemCodes.AI_OPTIMIZATION_NOTE, "retry-key", ResourceType.ALIYUN_CREDENTIAL, RESOURCE_ID);
        UsageMeteringResult retry = service.report(MeterItemCodes.AI_OPTIMIZATION_NOTE, "retry-key", ResourceType.ALIYUN_CREDENTIAL, RESOURCE_ID);

        ArgumentCaptor<UsageEventRequest> captor = ArgumentCaptor.forClass(UsageEventRequest.class);
        verify(gateway, times(2)).reportUsage(captor.capture());
        assertThat(captor.getAllValues()).extracting(UsageEventRequest::idempotencyKey).containsOnly("retry-key");
        assertThat(captor.getAllValues()).extracting(UsageEventRequest::eventId).containsOnly(captor.getAllValues().get(0).eventId());
        assertThat(retry.decision()).isEqualTo(UsageDecision.REJECT);
        assertThat(retry.reason()).isEqualTo("QUOTA_EXCEEDED");
    }
}
