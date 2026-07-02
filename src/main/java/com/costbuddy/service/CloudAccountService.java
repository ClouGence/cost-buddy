package com.costbuddy.service;

import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.mapper.CloudAccountMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloudAccountService {

    private static final String RESOURCE_NAME = "cloud_account";

    private final CloudAccountMapper cloudAccountMapper;

    public CloudAccountService(CloudAccountMapper cloudAccountMapper) {
        this.cloudAccountMapper = cloudAccountMapper;
    }

    @Transactional
    public CloudAccountDO create(CloudAccountRequest request) {
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        normalize(cloudAccount);
        cloudAccountMapper.insert(cloudAccount);
        return get(cloudAccount.getId());
    }

    public CloudAccountDO get(Long id) {
        CloudAccountDO cloudAccount = cloudAccountMapper.selectById(id);
        if (cloudAccount == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return cloudAccount;
    }

    public List<CloudAccountDO> list() {
        return cloudAccountMapper.selectAll();
    }

    @Transactional
    public CloudAccountDO update(Long id, CloudAccountRequest request) {
        CloudAccountDO existing = get(id);
        CloudAccountDO cloudAccount = new CloudAccountDO();
        BeanUtils.copyProperties(request, cloudAccount);
        cloudAccount.setId(id);
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
        cloudAccountMapper.deleteById(id);
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
