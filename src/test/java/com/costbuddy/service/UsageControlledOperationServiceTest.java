package com.costbuddy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.costbuddy.ai.AiChatClient;
import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.domain.AiEngineDO;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.BillingAuditRunRequest;
import com.costbuddy.dto.response.MeteredOperationResponse;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingAuditRunMapper;
import com.costbuddy.mapper.BillingItemExplanationMapper;
import com.costbuddy.metering.UsageMeteringResult;
import com.costbuddy.metering.UsageMeteringService;
import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.UsageDecision;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsageControlledOperationServiceTest {

    private static final long   RUN_ID      = 10L;
    private static final long   ITEM_ID     = 20L;
    private static final long   ACCOUNT_ID  = 30L;
    private static final String RESOURCE_ID = "aliyun-credential:test";

    @Test
    void rejectedAuditUsageDoesNotStartCollection() {
        CloudAccountService cloudAccountService = mock(CloudAccountService.class);
        AliyunBillingRawLineCollector collector = mock(AliyunBillingRawLineCollector.class);
        BillingAuditItemAggregator aggregator = mock(BillingAuditItemAggregator.class);
        BillingAuditRunMapper runMapper = mock(BillingAuditRunMapper.class);
        UsageMeteringService meteringService = rejectedMetering();
        when(cloudAccountService.get(ACCOUNT_ID)).thenReturn(cloudAccount());
        BillingAuditService service = new BillingAuditService(cloudAccountService,
            collector,
            aggregator,
            runMapper,
            mock(BillingAuditItemMapper.class),
            mock(BillingAuditRawLineMapper.class),
            mock(BillingItemRuleMatcher.class),
            mock(BillingItemRuleService.class),
            mock(CurrentUserProvider.class),
            meteringService);
        BillingAuditRunRequest request = new BillingAuditRunRequest();
        request.setCloudAccountId(ACCOUNT_ID);
        request.setBillDate(LocalDate.of(2026, 7, 10));
        request.setIdempotencyKey("audit-retry-key");

        MeteredOperationResponse<BillingAuditRunDO> response = service.trigger(request);

        assertThat(response.getDecision()).isEqualTo(UsageDecision.REJECT);
        assertThat(response.getReason()).isEqualTo("QUOTA_EXCEEDED");
        assertThat(response.getResult()).isNull();
        verifyNoInteractions(collector, aggregator, runMapper);
    }

    @Test
    void rejectedAiUsageDoesNotCallAiEngine() {
        BillingAuditService auditService = mock(BillingAuditService.class);
        BillingAuditItemMapper itemMapper = mock(BillingAuditItemMapper.class);
        BillingAuditRawLineMapper rawLineMapper = mock(BillingAuditRawLineMapper.class);
        BillingItemExplanationMapper explanationMapper = mock(BillingItemExplanationMapper.class);
        AiEngineService aiEngineService = mock(AiEngineService.class);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        CloudAccountService cloudAccountService = mock(CloudAccountService.class);
        BillingAuditRunDO run = new BillingAuditRunDO();
        run.setId(RUN_ID);
        run.setCloudAccountId(ACCOUNT_ID);
        BillingAuditItemDO item = new BillingAuditItemDO();
        item.setId(ITEM_ID);
        item.setRunId(RUN_ID);
        when(auditService.get(RUN_ID)).thenReturn(run);
        when(itemMapper.selectById(ITEM_ID)).thenReturn(item);
        when(cloudAccountService.get(ACCOUNT_ID)).thenReturn(cloudAccount());
        when(aiEngineService.get(40L)).thenReturn(new AiEngineDO());
        when(rawLineMapper.selectResourcesByAuditItem(item)).thenReturn(List.of());
        BillingItemExplanationService service = new BillingItemExplanationService(itemMapper,
            rawLineMapper,
            explanationMapper,
            auditService,
            aiEngineService,
            aiChatClient,
            cloudAccountService,
            rejectedMetering());

        MeteredOperationResponse<?> response = service.explain(RUN_ID, ITEM_ID, 40L, "ai-retry-key");

        assertThat(response.getDecision()).isEqualTo(UsageDecision.REJECT);
        assertThat(response.getResult()).isNull();
        verifyNoInteractions(aiChatClient, explanationMapper);
    }

    private UsageMeteringService rejectedMetering() {
        UsageMeteringService service = mock(UsageMeteringService.class);
        when(service.report(anyString(), anyString(), eq(ResourceType.ALIYUN_CREDENTIAL), eq(RESOURCE_ID)))
            .thenReturn(new UsageMeteringResult(UsageDecision.REJECT, "QUOTA_EXCEEDED"));
        return service;
    }

    private CloudAccountDO cloudAccount() {
        CloudAccountDO account = new CloudAccountDO();
        account.setId(ACCOUNT_ID);
        account.setCredentialResourceId(RESOURCE_ID);
        return account;
    }
}
