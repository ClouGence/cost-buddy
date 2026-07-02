package com.costbuddy.service;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BillingRawLineCollectionResult {

    private int        rawLineCount;
    private BigDecimal totalPretaxAmount = BigDecimal.ZERO;

    public void addPretaxAmount(BigDecimal pretaxAmount) {
        if (pretaxAmount == null) {
            return;
        }
        totalPretaxAmount = totalPretaxAmount.add(pretaxAmount);
    }
}
