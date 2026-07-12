package com.costbuddy.service;

import com.costbuddy.aliyun.AliyunBssOpenApiClient;
import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.dto.response.CloudAccountCheckResponse;
import com.costbuddy.mapper.CloudAccountMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloudAccountService {

    private static final String          RESOURCE_NAME = "cloud_account";

    private final CloudAccountMapper     cloudAccountMapper;
    private final AliyunBssOpenApiClient aliyunBssOpenApiClient;
    private final CurrentUserProvider    currentUserProvider;

    public CloudAccountService(CloudAccountMapper cloudAccountMapper, AliyunBssOpenApiClient aliyunBssOpenApiClient, CurrentUserProvider currentUserProvider){
        this.cloudAccountMapper = cloudAccountMapper;
        this.aliyunBssOpenApiClient = aliyunBssOpenApiClient;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public CloudAccountDO create(CloudAccountRequest request) {
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        cloudAccount.setMotherboardUserId(currentUserProvider.motherboardUserId());
        normalize(cloudAccount);
        cloudAccountMapper.insert(cloudAccount);
        return get(cloudAccount.getId());
    }

    public CloudAccountDO get(Long id) {
        CloudAccountDO cloudAccount = cloudAccountMapper.selectByIdAndMotherboardUserId(id, currentUserProvider.motherboardUserId());
        if (cloudAccount == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return cloudAccount;
    }

    public List<CloudAccountDO> list() {
        return cloudAccountMapper.selectAllByMotherboardUserId(currentUserProvider.motherboardUserId());
    }

    public CloudAccountCheckResponse check(Long id) {
        return aliyunBssOpenApiClient.checkAccess(get(id));
    }

    @Transactional
    public CloudAccountDO update(Long id, CloudAccountRequest request) {
        CloudAccountDO existing = get(id);
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        cloudAccount.setId(id);
        cloudAccount.setMotherboardUserId(existing.getMotherboardUserId());
        if (isBlank(request.getAccessKeySecret())) {
            cloudAccount.setAccessKeySecret(existing.getAccessKeySecret());
        }
        normalize(cloudAccount);
        cloudAccountMapper.update(cloudAccount);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        cloudAccountMapper.deleteByIdAndMotherboardUserId(id, currentUserProvider.motherboardUserId());
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
