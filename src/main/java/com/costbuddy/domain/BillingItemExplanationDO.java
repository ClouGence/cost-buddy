package com.costbuddy.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillingItemExplanationDO {

    private Long          id;
    private Long          auditItemId;
    private Long          aiEngineId;
    private String        promptContext;
    private String        explanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
