package com.costbuddy.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillingItemRuleDO {

    private Long          id;
    private Long          motherboardUserId;
    private String        provider;
    private String        matchScope;
    private String        productCode;
    private String        productName;
    private String        productDetail;
    private String        commodityCode;
    private String        billingItemCode;
    private String        billingItem;
    private String        decision;
    private String        note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
