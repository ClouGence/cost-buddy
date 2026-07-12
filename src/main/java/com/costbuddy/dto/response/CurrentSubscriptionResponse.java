package com.costbuddy.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentSubscriptionResponse {

    private boolean       subscribed;
    private Long          subscriptionId;
    private Long          planId;
    private String        planName;
    private String        status;
    private LocalDateTime currentPeriodEnd;
    private Boolean       autoRenew;
}
