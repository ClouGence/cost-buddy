package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class BillingAuditRunRequest {

    @NotNull
    private Long      cloudAccountId;

    @NotNull
    private LocalDate billDate;

    @NotBlank
    @Size(max = 256)
    private String    idempotencyKey;

    private LocalDate periodStartDate;

    private LocalDate periodEndDate;
}
