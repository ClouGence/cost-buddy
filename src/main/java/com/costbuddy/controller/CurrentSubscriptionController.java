package com.costbuddy.controller;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.dto.response.CurrentSubscriptionResponse;
import com.costbuddy.subscription.CurrentSubscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/subscription")
public class CurrentSubscriptionController {

    private final CurrentSubscriptionService currentSubscriptionService;

    public CurrentSubscriptionController(CurrentSubscriptionService currentSubscriptionService){
        this.currentSubscriptionService = currentSubscriptionService;
    }

    @GetMapping
    public ApiResponse<CurrentSubscriptionResponse> getCurrent() { return ApiResponse.ok(currentSubscriptionService.getCurrent()); }
}
