package com.costbuddy.auth;

import java.io.Serializable;

public record CurrentUser(Long motherboardUserId, String displayName, String email) implements Serializable {
}
