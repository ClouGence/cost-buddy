package com.costbuddy.motherboard;

import lombok.Getter;

@Getter
public class MotherboardGatewayException extends RuntimeException {

    private final MotherboardFailureType failureType;
    private final Integer                upstreamStatusCode;
    private final String                 upstreamCode;

    MotherboardGatewayException(MotherboardFailureType failureType, String message, Integer upstreamStatusCode, String upstreamCode, Throwable cause){
        super(message, cause);
        this.failureType = failureType;
        this.upstreamStatusCode = upstreamStatusCode;
        this.upstreamCode = upstreamCode;
    }
}
