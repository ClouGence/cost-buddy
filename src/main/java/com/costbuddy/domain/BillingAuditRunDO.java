package com.costbuddy.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillingAuditRunDO {

    private Long          id;
    private Long          motherboardUserId;
    private Long          cloudAccountId;
    private String        status;
    private LocalDate     billDate;
    private LocalDate     periodStartDate;
    private LocalDate     periodEndDate;
    private Integer       itemCount;
    private Integer       unknownItemCount;
    private BigDecimal    totalPretaxAmount;
    private BigDecimal    unknownPretaxAmount;
    private String        message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
