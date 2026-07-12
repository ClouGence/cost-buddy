package com.costbuddy.permission;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCredentialPermissionService implements CredentialPermissionService {

    @Override
    public void grant(long userId, String credentialResourceId) {
    }

    @Override
    public void requireRead(long userId, String credentialResourceId) {
    }

    @Override
    public void requireWrite(long userId, String credentialResourceId) {
    }

    @Override
    public void revoke(long userId, String credentialResourceId) {
    }
}
