package com.costbuddy.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillingAuditItemDO {

    private Long          id;
    private Long          runId;
    private String        provider;
    private String        productCode;
    private String        productName;
    private String        productDetail;
    private String        commodityCode;
    private String        billingItemCode;
    private String        billingItem;
    private String        billingType;
    private String        subscriptionType;
    private String        currency;
    private BigDecimal    stableDayPretaxAmount;
    private BigDecimal    periodPretaxAmount;
    private Integer       instanceCount;
    private Integer       regionCount;
    private String        sampleInstanceId;
    private String        sampleRegion;
    private String        sampleUsage;
    private String        sampleUsageUnit;
    private String        decision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
