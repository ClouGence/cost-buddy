package com.costbuddy.permission;

public interface CredentialPermissionService {

    void grant(long userId, String credentialResourceId);

    void requireRead(long userId, String credentialResourceId);

    void requireWrite(long userId, String credentialResourceId);

    void revoke(long userId, String credentialResourceId);
}
