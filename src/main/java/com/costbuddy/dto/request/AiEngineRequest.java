package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiEngineRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String model;

    private String apiKey;

    @NotBlank
    private String apiAddr;
}
