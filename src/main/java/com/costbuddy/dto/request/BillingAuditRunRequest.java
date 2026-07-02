package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class BillingAuditRunRequest {

    @NotNull
    private Long cloudAccountId;

    @NotNull
    private LocalDate billDate;

    private LocalDate periodStartDate;

    private LocalDate periodEndDate;
}
