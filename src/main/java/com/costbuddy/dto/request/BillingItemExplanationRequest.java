package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillingItemExplanationRequest {

    @NotNull
    private Long aiEngineId;
}
