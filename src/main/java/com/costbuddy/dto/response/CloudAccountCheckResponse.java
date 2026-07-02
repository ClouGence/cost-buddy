package com.costbuddy.dto.response;

import lombok.Data;

@Data
public class CloudAccountCheckResponse {

    private boolean available;
    private String  message;
    private String  requestId;
    private String  billingCycle;
    private String  accountId;
    private String  accountName;
    private Integer totalCount;

    public static CloudAccountCheckResponse available(String message) {
        CloudAccountCheckResponse response = new CloudAccountCheckResponse();
        response.setAvailable(true);
        response.setMessage(message);
        return response;
    }

    public static CloudAccountCheckResponse unavailable(String message) {
        CloudAccountCheckResponse response = new CloudAccountCheckResponse();
        response.setAvailable(false);
        response.setMessage(message);
        return response;
    }
}
