package com.costbuddy.aliyun;

import com.aliyun.bssopenapi20171214.Client;
import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillRequest;
import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillResponse;
import com.aliyun.bssopenapi20171214.models.QueryAccountBillRequest;
import com.aliyun.bssopenapi20171214.models.QueryAccountBillResponse;
import com.aliyun.bssopenapi20171214.models.QueryAccountBillResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.response.CloudAccountCheckResponse;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class AliyunBssOpenApiClient {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ENDPOINT = "business.aliyuncs.com";

    public CloudAccountCheckResponse checkAccess(CloudAccountDO cloudAccount) {
        CloudAccountCheckResponse validationResult = validateCredential(cloudAccount);
        if (validationResult != null) {
            return validationResult;
        }
        String billingCycle = YearMonth.now(SHANGHAI_ZONE).toString();
        QueryAccountBillRequest request = new QueryAccountBillRequest()
            .setBillingCycle(billingCycle)
            .setPageNum(1)
            .setPageSize(1)
            .setIsGroupByProduct(true);
        if (cloudAccount.getBillOwnerId() != null) {
            request.setBillOwnerId(cloudAccount.getBillOwnerId());
        }
        try {
            QueryAccountBillResponse response = createClient(cloudAccount).queryAccountBill(request);
            return toCheckResponse(response, billingCycle);
        } catch (TeaException exception) {
            return CloudAccountCheckResponse.unavailable(formatTeaException(exception));
        } catch (Exception exception) {
            return CloudAccountCheckResponse.unavailable(exception.getMessage());
        }
    }

    public DescribeInstanceBillResponse describeInstanceBill(CloudAccountDO cloudAccount, DescribeInstanceBillRequest request) {
        CloudAccountCheckResponse validationResult = validateCredential(cloudAccount);
        if (validationResult != null) {
            throw new BusinessException("ALIYUN_CREDENTIAL_INVALID", validationResult.getMessage());
        }
        try {
            return createClient(cloudAccount).describeInstanceBill(request);
        } catch (TeaException exception) {
            throw new BusinessException("ALIYUN_BSS_REQUEST_FAILED", formatTeaException(exception));
        } catch (Exception exception) {
            throw new BusinessException("ALIYUN_BSS_REQUEST_FAILED", exception.getMessage());
        }
    }

    private Client createClient(CloudAccountDO cloudAccount) throws Exception {
        Config config = new Config()
            .setAccessKeyId(cloudAccount.getAccessKeyId())
            .setAccessKeySecret(cloudAccount.getAccessKeySecret())
            .setEndpoint(ENDPOINT)
            .setConnectTimeout(10_000)
            .setReadTimeout(20_000);
        return new Client(config);
    }

    private CloudAccountCheckResponse toCheckResponse(QueryAccountBillResponse response, String billingCycle) {
        QueryAccountBillResponseBody body = response.getBody();
        if (body == null) {
            return CloudAccountCheckResponse.unavailable("BSS OpenAPI response body is empty");
        }
        if (!Boolean.TRUE.equals(body.getSuccess())) {
            return CloudAccountCheckResponse.unavailable(formatBssFailure(body));
        }
        QueryAccountBillResponseBody.QueryAccountBillResponseBodyData data = body.getData();
        CloudAccountCheckResponse checkResponse = CloudAccountCheckResponse.available("BSS OpenAPI is available");
        checkResponse.setRequestId(body.getRequestId());
        checkResponse.setBillingCycle(billingCycle);
        if (data != null) {
            checkResponse.setAccountId(data.getAccountID());
            checkResponse.setAccountName(data.getAccountName());
            checkResponse.setTotalCount(data.getTotalCount());
        }
        return checkResponse;
    }

    private CloudAccountCheckResponse validateCredential(CloudAccountDO cloudAccount) {
        if (!"ALIYUN".equals(cloudAccount.getProvider())) {
            return CloudAccountCheckResponse.unavailable("unsupported cloud provider: " + cloudAccount.getProvider());
        }
        if (isBlank(cloudAccount.getAccessKeyId()) || isBlank(cloudAccount.getAccessKeySecret())) {
            return CloudAccountCheckResponse.unavailable("Alibaba Cloud access key id and secret are required");
        }
        return null;
    }

    private String formatBssFailure(QueryAccountBillResponseBody body) {
        String code = body.getCode();
        String message = body.getMessage();
        if (isBlank(code)) {
            return message;
        }
        if (isBlank(message)) {
            return code;
        }
        return code + ": " + message;
    }

    private String formatTeaException(TeaException exception) {
        String code = exception.getCode();
        String message = exception.getMessage();
        if (isBlank(code)) {
            return message;
        }
        if (isBlank(message)) {
            return code;
        }
        return code + ": " + message;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
