package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BillingAuditItemRuleRequest {

    @NotBlank
    private String matchScope;

    @NotBlank
    private String decision;

    private String note;
}
