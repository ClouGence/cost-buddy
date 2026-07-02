package com.costbuddy.controller;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.dto.request.BillingAuditRunRequest;
import com.costbuddy.dto.response.BillingAuditItemResourceResponse;
import com.costbuddy.service.BillingAuditService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing-audits")
public class BillingAuditController {

    private final BillingAuditService billingAuditService;

    public BillingAuditController(BillingAuditService billingAuditService) {
        this.billingAuditService = billingAuditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BillingAuditRunDO> trigger(@Valid @RequestBody BillingAuditRunRequest request) {
        return ApiResponse.ok(billingAuditService.trigger(request));
    }

    @GetMapping
    public ApiResponse<List<BillingAuditRunDO>> list() {
        return ApiResponse.ok(billingAuditService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BillingAuditRunDO> get(@PathVariable Long id) {
        return ApiResponse.ok(billingAuditService.get(id));
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<BillingAuditItemDO>> listItems(@PathVariable Long id) {
        return ApiResponse.ok(billingAuditService.listItems(id));
    }

    @GetMapping("/{id}/items/{itemId}/resources")
    public ApiResponse<List<BillingAuditItemResourceResponse>> listItemResources(@PathVariable Long id, @PathVariable Long itemId) {
        return ApiResponse.ok(billingAuditService.listItemResources(id, itemId));
    }
}
