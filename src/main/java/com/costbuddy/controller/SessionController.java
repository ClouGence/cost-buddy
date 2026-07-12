package com.costbuddy.controller;

import com.costbuddy.auth.AuthenticationProperties;
import com.costbuddy.auth.CurrentUser;
import com.costbuddy.auth.CurrentUserContext;
import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.dto.response.AuthenticationConfigResponse;
import com.costbuddy.motherboard.MotherboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final MotherboardProperties    motherboardProperties;
    private final AuthenticationProperties authenticationProperties;
    private final CurrentUserContext       currentUserContext;

    public SessionController(MotherboardProperties motherboardProperties, AuthenticationProperties authenticationProperties, CurrentUserContext currentUserContext){
        this.motherboardProperties = motherboardProperties;
        this.authenticationProperties = authenticationProperties;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping("/api/auth/config")
    public ApiResponse<AuthenticationConfigResponse> config() {
        return ApiResponse.ok(new AuthenticationConfigResponse(motherboardProperties.isEnabled(), authenticationProperties.configuredProviders()));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        currentUserContext.clear(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/me")
    public ApiResponse<CurrentUser> me(HttpServletRequest request) {
        return ApiResponse.ok(currentUserContext.require(request));
    }
}
