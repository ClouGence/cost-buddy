package com.costbuddy.service;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BillingAuditAggregationResult {

    private int        itemCount;
    private int        unknownItemCount;
    private BigDecimal totalPretaxAmount = BigDecimal.ZERO;
    private BigDecimal unknownPretaxAmount = BigDecimal.ZERO;

    public void addItem(BigDecimal periodPretaxAmount, String decision) {
        itemCount++;
        if (periodPretaxAmount != null) {
            totalPretaxAmount = totalPretaxAmount.add(periodPretaxAmount);
        }
        if ("UNKNOWN".equals(decision)) {
            unknownItemCount++;
            if (periodPretaxAmount != null) {
                unknownPretaxAmount = unknownPretaxAmount.add(periodPretaxAmount);
            }
        }
    }
}
