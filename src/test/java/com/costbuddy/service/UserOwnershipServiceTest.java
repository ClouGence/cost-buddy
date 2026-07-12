package com.costbuddy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.aliyun.AliyunBssOpenApiClient;
import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.BillingItemRuleRequest;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingAuditRunMapper;
import com.costbuddy.mapper.BillingItemRuleMapper;
import com.costbuddy.mapper.CloudAccountMapper;
import com.costbuddy.permission.CredentialPermissionService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserOwnershipServiceTest {

    private static final long   USER_ID = 42L;

    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.motherboardUserId()).thenReturn(USER_ID);
    }

    @Test
    void assignsAndQueriesCloudAccountForCurrentUser() {
        CloudAccountMapper mapper = mock(CloudAccountMapper.class);
        AtomicReference<CloudAccountDO> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            CloudAccountDO account = invocation.getArgument(0);
            account.setId(1L);
            inserted.set(account);
            return 1;
        }).when(mapper).insert(any());
        when(mapper.selectByIdAndMotherboardUserId(1L, USER_ID)).thenAnswer(invocation -> inserted.get());
        CredentialPermissionService permissionService = mock(CredentialPermissionService.class);
        CloudAccountService service = new CloudAccountService(mapper, mock(AliyunBssOpenApiClient.class), currentUserProvider, permissionService);
        CloudAccountRequest request = new CloudAccountRequest();
        request.setName("account");
        request.setProvider("ALIYUN");

        CloudAccountDO account = service.create(request);

        assertThat(account.getMotherboardUserId()).isEqualTo(USER_ID);
        assertThat(account.getCredentialResourceId()).startsWith("aliyun-credential:");
        verify(permissionService).grant(USER_ID, account.getCredentialResourceId());
        verify(mapper).selectByIdAndMotherboardUserId(1L, USER_ID);
    }

    @Test
    void assignsAndListsRulesForCurrentUser() {
        BillingItemRuleMapper mapper = mock(BillingItemRuleMapper.class);
        AtomicReference<BillingItemRuleDO> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            BillingItemRuleDO rule = invocation.getArgument(0);
            rule.setId(2L);
            inserted.set(rule);
            return 1;
        }).when(mapper).insert(any());
        when(mapper.selectByIdAndMotherboardUserId(2L, USER_ID)).thenAnswer(invocation -> inserted.get());
        when(mapper.selectAllByMotherboardUserId(USER_ID)).thenAnswer(invocation -> List.of(inserted.get()));
        BillingItemRuleService service = new BillingItemRuleService(mapper, currentUserProvider);
        BillingItemRuleRequest request = new BillingItemRuleRequest();
        request.setProvider("ALIYUN");
        request.setMatchScope("BILLING_ITEM");
        request.setBillingItemCode("item-code");
        request.setDecision("IGNORED");

        BillingItemRuleDO rule = service.create(request);

        assertThat(rule.getMotherboardUserId()).isEqualTo(USER_ID);
        assertThat(service.list()).containsExactly(rule);
        verify(mapper).selectAllByMotherboardUserId(USER_ID);
    }

    @Test
    void filtersAuditRunsAndHidesOtherUsersIds() {
        BillingAuditRunMapper runMapper = mock(BillingAuditRunMapper.class);
        BillingAuditRunDO run = new BillingAuditRunDO();
        run.setId(4L);
        run.setMotherboardUserId(USER_ID);
        when(runMapper.selectAllByMotherboardUserId(USER_ID)).thenReturn(List.of(run));
        BillingAuditService service = billingAuditService(runMapper);

        assertThat(service.list()).containsExactly(run);
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
        verify(runMapper).selectByIdAndMotherboardUserId(99L, USER_ID);
    }

    private BillingAuditService billingAuditService(BillingAuditRunMapper runMapper) {
        return new BillingAuditService(mock(CloudAccountService.class),
            mock(AliyunBillingRawLineCollector.class),
            mock(BillingAuditItemAggregator.class),
            runMapper,
            mock(BillingAuditItemMapper.class),
            mock(BillingAuditRawLineMapper.class),
            mock(BillingItemRuleMatcher.class),
            mock(BillingItemRuleService.class),
            currentUserProvider);
    }
}
