package com.costbuddy.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillingAuditRawLineDO {

    private Long          id;
    private Long          runId;
    private String        provider;
    private String        sourceApi;
    private String        sourceBillingCycle;
    private LocalDate     billDate;
    private String        lineHash;
    private Long          billAccountId;
    private Long          billOwnerId;
    private Long          payerAccountId;
    private String        productCode;
    private String        productName;
    private String        productDetailCode;
    private String        productDetail;
    private String        commodityCode;
    private String        commodityName;
    private String        billingItemCode;
    private String        billingItem;
    private String        billingType;
    private String        subscriptionType;
    private String        instanceId;
    private String        instanceName;
    private String        region;
    private String        zone;
    private String        resourceGroup;
    private String        costUnit;
    private BigDecimal    usageAmount;
    private String        usageUnit;
    private BigDecimal    pretaxGrossAmount;
    private BigDecimal    invoiceDiscount;
    private BigDecimal    deductibleAmount;
    private BigDecimal    pretaxAmount;
    private BigDecimal    cashAmount;
    private BigDecimal    paymentAmount;
    private BigDecimal    outstandingAmount;
    private String        currency;
    private String        tags;
    private String        rawPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
