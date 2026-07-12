package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BillingItemExplanationRequest {

    @NotNull
    private Long   aiEngineId;

    @NotBlank
    @Size(max = 256)
    private String idempotencyKey;
}
