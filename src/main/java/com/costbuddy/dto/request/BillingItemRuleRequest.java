package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BillingItemRuleRequest {

    private String provider;

    @NotBlank
    private String matchScope;

    private String productCode;
    private String productName;
    private String productDetail;
    private String commodityCode;
    private String billingItemCode;
    private String billingItem;

    @NotBlank
    private String decision;

    private String note;
}
