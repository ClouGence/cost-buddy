package com.costbuddy.service;

import com.costbuddy.aliyun.AliyunBssOpenApiClient;
import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.dto.response.CloudAccountCheckResponse;
import com.costbuddy.mapper.CloudAccountMapper;
import com.costbuddy.permission.CredentialPermissionService;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloudAccountService {

    private static final String               RESOURCE_NAME              = "cloud_account";
    private static final String               CREDENTIAL_RESOURCE_PREFIX = "aliyun-credential:";

    private final CloudAccountMapper          cloudAccountMapper;
    private final AliyunBssOpenApiClient      aliyunBssOpenApiClient;
    private final CurrentUserProvider         currentUserProvider;
    private final CredentialPermissionService credentialPermissionService;

    public CloudAccountService(CloudAccountMapper cloudAccountMapper, AliyunBssOpenApiClient aliyunBssOpenApiClient, CurrentUserProvider currentUserProvider,
                               CredentialPermissionService credentialPermissionService){
        this.cloudAccountMapper = cloudAccountMapper;
        this.aliyunBssOpenApiClient = aliyunBssOpenApiClient;
        this.currentUserProvider = currentUserProvider;
        this.credentialPermissionService = credentialPermissionService;
    }

    public CloudAccountDO create(CloudAccountRequest request) {
        long userId = currentUserProvider.motherboardUserId();
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        cloudAccount.setMotherboardUserId(userId);
        cloudAccount.setCredentialResourceId(CREDENTIAL_RESOURCE_PREFIX + UUID.randomUUID());
        normalize(cloudAccount);
        cloudAccountMapper.insert(cloudAccount);
        try {
            credentialPermissionService.grant(userId, cloudAccount.getCredentialResourceId());
        } catch (RuntimeException exception) {
            deleteCreatedAccount(cloudAccount, exception);
            throw exception;
        }
        return getOwned(cloudAccount.getId(), userId);
    }

    public CloudAccountDO get(Long id) {
        long userId = currentUserProvider.motherboardUserId();
        CloudAccountDO cloudAccount = getOwned(id, userId);
        credentialPermissionService.requireRead(userId, cloudAccount.getCredentialResourceId());
        return cloudAccount;
    }

    public List<CloudAccountDO> list() {
        long userId = currentUserProvider.motherboardUserId();
        List<CloudAccountDO> cloudAccounts = cloudAccountMapper.selectAllByMotherboardUserId(userId);
        cloudAccounts.forEach(cloudAccount -> credentialPermissionService.requireRead(userId, cloudAccount.getCredentialResourceId()));
        return cloudAccounts;
    }

    public CloudAccountCheckResponse check(Long id) {
        return aliyunBssOpenApiClient.checkAccess(get(id));
    }

    @Transactional
    public CloudAccountDO update(Long id, CloudAccountRequest request) {
        long userId = currentUserProvider.motherboardUserId();
        CloudAccountDO existing = getOwned(id, userId);
        credentialPermissionService.requireWrite(userId, existing.getCredentialResourceId());
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        cloudAccount.setId(id);
        cloudAccount.setMotherboardUserId(existing.getMotherboardUserId());
        cloudAccount.setCredentialResourceId(existing.getCredentialResourceId());
        if (isBlank(request.getAccessKeySecret())) {
            cloudAccount.setAccessKeySecret(existing.getAccessKeySecret());
        }
        normalize(cloudAccount);
        cloudAccountMapper.update(cloudAccount);
        return getOwned(id, userId);
    }

    public void delete(Long id) {
        long userId = currentUserProvider.motherboardUserId();
        CloudAccountDO cloudAccount = getOwned(id, userId);
        credentialPermissionService.requireWrite(userId, cloudAccount.getCredentialResourceId());
        credentialPermissionService.revoke(userId, cloudAccount.getCredentialResourceId());
        try {
            cloudAccountMapper.deleteByIdAndMotherboardUserId(id, userId);
        } catch (RuntimeException exception) {
            restoreRevokedPermission(userId, cloudAccount.getCredentialResourceId(), exception);
            throw exception;
        }
    }

    private CloudAccountDO getOwned(Long id, long userId) {
        CloudAccountDO cloudAccount = cloudAccountMapper.selectByIdAndMotherboardUserId(id, userId);
        if (cloudAccount == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return cloudAccount;
    }

    private void deleteCreatedAccount(CloudAccountDO cloudAccount, RuntimeException originalException) {
        try {
            cloudAccountMapper.deleteByIdAndMotherboardUserId(cloudAccount.getId(), cloudAccount.getMotherboardUserId());
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private void restoreRevokedPermission(long userId, String credentialResourceId, RuntimeException originalException) {
        try {
            credentialPermissionService.grant(userId, credentialResourceId);
        } catch (RuntimeException recoveryException) {
            originalException.addSuppressed(recoveryException);
        }
    }

    private void normalize(CloudAccountDO cloudAccount) {
        if (isBlank(cloudAccount.getProvider())) {
            cloudAccount.setProvider("ALIYUN");
        }
        if (cloudAccount.getEnabled() == null) {
            cloudAccount.setEnabled(true);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
