package com.costbuddy.permission;

import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.PermissionAction;
import com.motherboard.sdk.model.ResourceType;
import com.motherboard.sdk.model.request.AuthzCheckRequest;
import com.motherboard.sdk.model.request.PermissionGrantRequest;
import com.motherboard.sdk.model.response.AuthzCheckResponse;
import com.motherboard.sdk.model.response.PermissionGrant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class MotherboardCredentialPermissionService implements CredentialPermissionService {

    private static final Logger         LOGGER = LoggerFactory.getLogger(MotherboardCredentialPermissionService.class);

    private final MotherboardGateway    motherboardGateway;
    private final MotherboardProperties motherboardProperties;

    public MotherboardCredentialPermissionService(MotherboardGateway motherboardGateway, MotherboardProperties motherboardProperties){
        this.motherboardGateway = motherboardGateway;
        this.motherboardProperties = motherboardProperties;
    }

    @Override
    public void grant(long userId, String credentialResourceId) {
        List<Long> createdGrantIds = new ArrayList<>();
        try {
            createdGrantIds.add(grant(userId, credentialResourceId, PermissionAction.READ));
            createdGrantIds.add(grant(userId, credentialResourceId, PermissionAction.WRITE));
        } catch (RuntimeException exception) {
            revokeCreatedGrants(createdGrantIds, exception);
            throw exception;
        }
    }

    @Override
    public void requireRead(long userId, String credentialResourceId) {
        require(userId, credentialResourceId, PermissionAction.READ);
    }

    @Override
    public void requireWrite(long userId, String credentialResourceId) {
        require(userId, credentialResourceId, PermissionAction.WRITE);
    }

    @Override
    public void revoke(long userId, String credentialResourceId) {
        List<PermissionGrant> permissionGrants = Objects.requireNonNull(motherboardGateway.listPermissions(userId), "Motherboard returned an invalid permission grant list");
        List<PermissionGrant> grants = permissionGrants.stream()
            .filter(grant -> Objects.equals(grant.productId(), motherboardProperties.getProductId()))
            .filter(grant -> grant.resourceType() == ResourceType.ALIYUN_CREDENTIAL)
            .filter(grant -> Objects.equals(grant.resourceId(), credentialResourceId))
            .filter(grant -> grant.action() == PermissionAction.READ || grant.action() == PermissionAction.WRITE)
            .sorted(Comparator.comparingInt(grant -> grant.action() == PermissionAction.WRITE ? 1 : 0))
            .toList();
        List<PermissionGrant> revokedGrants = new ArrayList<>();
        try {
            for (PermissionGrant grant : grants) {
                if (grant.id() != null) {
                    motherboardGateway.revokePermission(grant.id());
                    revokedGrants.add(grant);
                }
            }
        } catch (RuntimeException exception) {
            restoreRevokedGrants(userId, credentialResourceId, revokedGrants, exception);
            throw exception;
        }
    }

    private long grant(long userId, String credentialResourceId, PermissionAction action) {
        PermissionGrant grant = motherboardGateway
            .grantPermission(new PermissionGrantRequest(userId, motherboardProperties.getProductId(), ResourceType.ALIYUN_CREDENTIAL, credentialResourceId, action));
        if (grant == null || grant.id() == null) {
            throw new IllegalStateException("Motherboard returned an invalid permission grant");
        }
        return grant.id();
    }

    private void require(long userId, String credentialResourceId, PermissionAction action) {
        AuthzCheckResponse result = motherboardGateway
            .checkAuthorization(new AuthzCheckRequest(userId, motherboardProperties.getProductId(), ResourceType.ALIYUN_CREDENTIAL, credentialResourceId, action));
        if (result == null || !result.allowed()) {
            throw new BusinessException("FORBIDDEN", "permission denied for Alibaba Cloud credential");
        }
    }

    private void revokeCreatedGrants(List<Long> grantIds, RuntimeException originalException) {
        for (int index = grantIds.size() - 1; index >= 0; index--) {
            try {
                motherboardGateway.revokePermission(grantIds.get(index));
            } catch (RuntimeException cleanupException) {
                originalException.addSuppressed(cleanupException);
                LOGGER.error("failed to clean up Motherboard permission grant: grantId={}", grantIds.get(index), cleanupException);
            }
        }
    }

    private void restoreRevokedGrants(long userId, String credentialResourceId, List<PermissionGrant> revokedGrants, RuntimeException originalException) {
        for (PermissionGrant revokedGrant : revokedGrants) {
            try {
                grant(userId, credentialResourceId, revokedGrant.action());
            } catch (RuntimeException recoveryException) {
                originalException.addSuppressed(recoveryException);
                LOGGER.error("failed to restore Motherboard permission grant: resourceId={}, action={}", credentialResourceId, revokedGrant.action(), recoveryException);
            }
        }
    }
}
