package com.costbuddy.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class BillingAuditItemResourceResponse {

    private Long       rawLineId;
    private LocalDate  billDate;
    private String     instanceId;
    private String     instanceName;
    private String     region;
    private String     zone;
    private String     resourceGroup;
    private String     costUnit;
    private String     billingType;
    private BigDecimal usageAmount;
    private String     usageUnit;
    private BigDecimal pretaxAmount;
    private String     currency;
}
