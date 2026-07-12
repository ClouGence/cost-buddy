package com.costbuddy.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.aliyun.AliyunBssOpenApiClient;
import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.mapper.CloudAccountMapper;
import com.costbuddy.permission.CredentialPermissionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloudAccountPermissionServiceTest {

    private static final long           USER_ID     = 42L;
    private static final long           ACCOUNT_ID  = 1L;
    private static final String         RESOURCE_ID = "aliyun-credential:test";

    private CloudAccountMapper          mapper;
    private CredentialPermissionService permissionService;
    private CloudAccountService         service;
    private CloudAccountDO              account;

    @BeforeEach
    void setUp() {
        mapper = mock(CloudAccountMapper.class);
        permissionService = mock(CredentialPermissionService.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.motherboardUserId()).thenReturn(USER_ID);
        service = new CloudAccountService(mapper, mock(AliyunBssOpenApiClient.class), currentUserProvider, permissionService);
        account = new CloudAccountDO();
        account.setId(ACCOUNT_ID);
        account.setMotherboardUserId(USER_ID);
        account.setCredentialResourceId(RESOURCE_ID);
        when(mapper.selectByIdAndMotherboardUserId(ACCOUNT_ID, USER_ID)).thenReturn(account);
    }

    @Test
    void requiresReadForGetAndList() {
        when(mapper.selectAllByMotherboardUserId(USER_ID)).thenReturn(List.of(account));

        service.get(ACCOUNT_ID);
        service.list();

        verify(permissionService, times(2)).requireRead(USER_ID, RESOURCE_ID);
    }

    @Test
    void requiresWriteForUpdateAndDelete() {
        service.update(ACCOUNT_ID, new CloudAccountRequest());
        service.delete(ACCOUNT_ID);

        verify(permissionService, times(2)).requireWrite(USER_ID, RESOURCE_ID);
        verify(permissionService).revoke(USER_ID, RESOURCE_ID);
        verify(mapper).deleteByIdAndMotherboardUserId(ACCOUNT_ID, USER_ID);
    }

    @Test
    void deletesCreatedAccountWhenPermissionGrantFails() {
        RuntimeException failure = new RuntimeException("grant failed");
        doThrow(failure).when(permissionService).grant(eq(USER_ID), anyString());
        doAnswer(invocation -> {
            CloudAccountDO inserted = invocation.getArgument(0);
            inserted.setId(ACCOUNT_ID);
            return 1;
        }).when(mapper).insert(any());

        assertThatThrownBy(() -> service.create(new CloudAccountRequest())).isSameAs(failure);

        verify(mapper).deleteByIdAndMotherboardUserId(ACCOUNT_ID, USER_ID);
    }

    @Test
    void restoresPermissionsWhenDatabaseDeleteFails() {
        RuntimeException failure = new RuntimeException("delete failed");
        doThrow(failure).when(mapper).deleteByIdAndMotherboardUserId(ACCOUNT_ID, USER_ID);

        assertThatThrownBy(() -> service.delete(ACCOUNT_ID)).isSameAs(failure);

        verify(permissionService).revoke(USER_ID, RESOURCE_ID);
        verify(permissionService).grant(USER_ID, RESOURCE_ID);
    }
}
