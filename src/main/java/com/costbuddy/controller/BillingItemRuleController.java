package com.costbuddy.controller;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.dto.request.BillingItemRuleRequest;
import com.costbuddy.service.BillingItemRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing-item-rules")
public class BillingItemRuleController {

    private final BillingItemRuleService billingItemRuleService;

    public BillingItemRuleController(BillingItemRuleService billingItemRuleService) {
        this.billingItemRuleService = billingItemRuleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BillingItemRuleDO> create(@Valid @RequestBody BillingItemRuleRequest request) {
        return ApiResponse.ok(billingItemRuleService.create(request));
    }

    @GetMapping
    public ApiResponse<List<BillingItemRuleDO>> list() {
        return ApiResponse.ok(billingItemRuleService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<BillingItemRuleDO> update(@PathVariable Long id, @Valid @RequestBody BillingItemRuleRequest request) {
        return ApiResponse.ok(billingItemRuleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        billingItemRuleService.delete(id);
        return ApiResponse.ok(null);
    }
}
