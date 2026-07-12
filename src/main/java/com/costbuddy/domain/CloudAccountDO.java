package com.costbuddy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CloudAccountDO {

    private Long          id;
    private Long          motherboardUserId;
    private String        name;
    private String        provider;
    private String        accessKeyId;
    @JsonIgnore
    private String        accessKeySecret;
    private Long          billOwnerId;
    private Boolean       enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
