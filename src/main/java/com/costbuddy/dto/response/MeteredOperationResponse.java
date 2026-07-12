package com.costbuddy.dto.response;

import com.costbuddy.metering.UsageMeteringResult;
import com.motherboard.sdk.model.UsageDecision;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeteredOperationResponse<T> {

    private UsageDecision decision;
    private String        reason;
    private T             result;

    public static <T> MeteredOperationResponse<T> allowed(UsageMeteringResult metering, T result) {
        return new MeteredOperationResponse<>(metering.decision(), metering.reason(), result);
    }

    public static <T> MeteredOperationResponse<T> rejected(UsageMeteringResult metering) {
        return new MeteredOperationResponse<>(metering.decision(), metering.reason(), null);
    }
}
