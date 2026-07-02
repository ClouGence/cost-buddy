package com.costbuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiEngineCheckResponse {

    private boolean available;
    private String  message;
}
