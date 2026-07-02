package com.costbuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloudAccountRequest {

    @NotBlank
    private String name;

    private String  provider;
    private String  accessKeyId;
    private String  accessKeySecret;
    private Long    billOwnerId;
    private Boolean enabled;
}
