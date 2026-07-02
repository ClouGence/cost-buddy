package com.costbuddy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiEngineDO {

    private Long          id;
    private String        name;
    private String        model;
    @JsonIgnore
    private String        apiKey;
    private String        apiAddr;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
