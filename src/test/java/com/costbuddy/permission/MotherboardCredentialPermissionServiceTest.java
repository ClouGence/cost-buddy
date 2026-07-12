package com.costbuddy.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.PermissionAction;
import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.request.AuthzCheckRequest;
import com.motherboard.sdk.model.request.PermissionGrantRequest;
import com.motherboard.sdk.model.response.AuthzCheckResponse;
import com.motherboard.sdk.model.response.PermissionGrant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class MotherboardCredentialPermissionServiceTest {

    private static final long                      USER_ID     = 42L;
    private static final long                      PRODUCT_ID  = 7L;
    private static final String                    RESOURCE_ID = "aliyun-credential:test";

    private MotherboardGateway                     gateway;
    private MotherboardCredentialPermissionService service;

    @BeforeEach
    void setUp() {
        gateway = mock(MotherboardGateway.class);
        MotherboardProperties properties = new MotherboardProperties();
        properties.setProductId(PRODUCT_ID);
        service = new MotherboardCredentialPermissionService(gateway, properties);
    }

    @Test
    void grantsReadAndWriteForAliyunCredential() {
        when(gateway.grantPermission(any())).thenReturn(grant(11L, PRODUCT_ID, RESOURCE_ID, PermissionAction.READ),
            grant(12L, PRODUCT_ID, RESOURCE_ID, PermissionAction.WRITE));

        service.grant(USER_ID, RESOURCE_ID);

        ArgumentCaptor<PermissionGrantRequest> captor = ArgumentCaptor.forClass(PermissionGrantRequest.class);
        verify(gateway, times(2)).grantPermission(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(request -> {
            assertThat(request.userId()).isEqualTo(USER_ID);
            assertThat(request.productId()).isEqualTo(PRODUCT_ID);
            assertThat(request.resourceType()).isEqualTo(ResourceType.ALIYUN_CREDENTIAL);
            assertThat(request.resourceId()).isEqualTo(RESOURCE_ID);
        }).extracting(PermissionGrantRequest::action).containsExactly(PermissionAction.READ, PermissionAction.WRITE);
    }

    @Test
    void cleansUpReadGrantWhenWriteGrantFails() {
        RuntimeException failure = new RuntimeException("grant failed");
        when(gateway.grantPermission(any())).thenReturn(grant(11L, PRODUCT_ID, RESOURCE_ID, PermissionAction.READ)).thenThrow(failure);

        assertThatThrownBy(() -> service.grant(USER_ID, RESOURCE_ID)).isSameAs(failure);

        verify(gateway).revokePermission(11L);
    }

    @Test
    void rejectsDeniedAuthorization() {
        when(gateway.checkAuthorization(any())).thenReturn(new AuthzCheckResponse(false, "denied"));

        assertThatThrownBy(() -> service.requireWrite(USER_ID, RESOURCE_ID))
            .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getCode()).isEqualTo("FORBIDDEN"));

        ArgumentCaptor<AuthzCheckRequest> captor = ArgumentCaptor.forClass(AuthzCheckRequest.class);
        verify(gateway).checkAuthorization(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(PermissionAction.WRITE);
    }

    @Test
    void revokesOnlyMatchingCredentialGrantsWithWriteLast() {
        when(gateway.listPermissions(USER_ID)).thenReturn(List.of(
            grant(1L, PRODUCT_ID, RESOURCE_ID, PermissionAction.WRITE),
            grant(2L, PRODUCT_ID, RESOURCE_ID, PermissionAction.READ),
            grant(3L, PRODUCT_ID + 1, RESOURCE_ID, PermissionAction.READ),
            grant(4L, PRODUCT_ID, "another-resource", PermissionAction.READ)));

        service.revoke(USER_ID, RESOURCE_ID);

        InOrder order = inOrder(gateway);
        order.verify(gateway).revokePermission(2L);
        order.verify(gateway).revokePermission(1L);
    }

    @Test
    void restoresReadGrantWhenWriteRevokeFails() {
        RuntimeException failure = new RuntimeException("revoke failed");
        when(gateway.listPermissions(USER_ID))
            .thenReturn(List.of(grant(1L, PRODUCT_ID, RESOURCE_ID, PermissionAction.WRITE), grant(2L, PRODUCT_ID, RESOURCE_ID, PermissionAction.READ)));
        doThrow(failure).when(gateway).revokePermission(1L);
        when(gateway.grantPermission(any())).thenReturn(grant(3L, PRODUCT_ID, RESOURCE_ID, PermissionAction.READ));

        assertThatThrownBy(() -> service.revoke(USER_ID, RESOURCE_ID)).isSameAs(failure);

        ArgumentCaptor<PermissionGrantRequest> captor = ArgumentCaptor.forClass(PermissionGrantRequest.class);
        verify(gateway).grantPermission(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(PermissionAction.READ);
    }

    private PermissionGrant grant(Long id, Long productId, String resourceId, PermissionAction action) {
        return new PermissionGrant(id, USER_ID, productId, ResourceType.ALIYUN_CREDENTIAL, resourceId, action, "ACTIVE", null, null, null, null, null);
    }
}
