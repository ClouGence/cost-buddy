package com.costbuddy.auth;

import com.costbuddy.motherboard.MotherboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public static final long            LOCAL_USER_ID = 0L;

    private final MotherboardProperties motherboardProperties;
    private final CurrentUserContext    currentUserContext;
    private final HttpServletRequest    request;

    public CurrentUserProvider(MotherboardProperties motherboardProperties, CurrentUserContext currentUserContext, HttpServletRequest request){
        this.motherboardProperties = motherboardProperties;
        this.currentUserContext = currentUserContext;
        this.request = request;
    }

    public long motherboardUserId() {
        if (!motherboardProperties.isEnabled()) {
            return LOCAL_USER_ID;
        }
        Long userId = currentUserContext.require(request).motherboardUserId();
        if (userId == null || userId <= 0) {
            throw new AuthenticationRequiredException();
        }
        return userId;
    }
}
