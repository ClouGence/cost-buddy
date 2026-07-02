package com.costbuddy.service;

import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillRequest;
import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillResponse;
import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillResponseBody;
import com.aliyun.teautil.Common;
import com.costbuddy.aliyun.AliyunBssOpenApiClient;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.domain.BillingAuditRawLineDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AliyunBillingRawLineCollector {

    private static final int MAX_RESULTS = 300;

    private final AliyunBssOpenApiClient   aliyunBssOpenApiClient;
    private final BillingAuditRawLineMapper billingAuditRawLineMapper;

    public AliyunBillingRawLineCollector(AliyunBssOpenApiClient aliyunBssOpenApiClient, BillingAuditRawLineMapper billingAuditRawLineMapper) {
        this.aliyunBssOpenApiClient = aliyunBssOpenApiClient;
        this.billingAuditRawLineMapper = billingAuditRawLineMapper;
    }

    public BillingRawLineCollectionResult collectStableDay(BillingAuditRunDO run, CloudAccountDO cloudAccount) {
        billingAuditRawLineMapper.deleteByRunId(run.getId());
        BillingRawLineCollectionResult result = new BillingRawLineCollectionResult();
        String billingCycle = YearMonth.from(run.getBillDate()).toString();
        String billingDate = run.getBillDate().toString();
        String nextToken = null;
        int sequence = 0;
        do {
            DescribeInstanceBillResponseBody body = describePage(cloudAccount, billingCycle, billingDate, nextToken);
            DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyData data = body.getData();
            if (data == null) {
                break;
            }
            List<DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyDataItems> items = data.getItems();
            if (items != null) {
                for (DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyDataItems item : items) {
                    sequence++;
                    BillingAuditRawLineDO rawLine = toRawLine(run, cloudAccount, item, billingCycle, sequence);
                    billingAuditRawLineMapper.insert(rawLine);
                    result.setRawLineCount(result.getRawLineCount() + 1);
                    result.addPretaxAmount(rawLine.getPretaxAmount());
                }
            }
            nextToken = data.getNextToken();
        } while (!isBlank(nextToken));
        return result;
    }

    private DescribeInstanceBillResponseBody describePage(CloudAccountDO cloudAccount, String billingCycle, String billingDate, String nextToken) {
        DescribeInstanceBillRequest request = new DescribeInstanceBillRequest()
            .setBillingCycle(billingCycle)
            .setBillingDate(billingDate)
            .setGranularity("DAILY")
            .setSubscriptionType("PayAsYouGo")
            .setIsBillingItem(true)
            .setIsHideZeroCharge(false)
            .setMaxResults(MAX_RESULTS)
            .setNextToken(nextToken);
        if (cloudAccount.getBillOwnerId() != null) {
            request.setBillOwnerId(cloudAccount.getBillOwnerId());
        }
        DescribeInstanceBillResponse response = aliyunBssOpenApiClient.describeInstanceBill(cloudAccount, request);
        DescribeInstanceBillResponseBody body = response.getBody();
        if (body == null) {
            throw new BusinessException("ALIYUN_BILLING_RESPONSE_EMPTY", "DescribeInstanceBill response body is empty");
        }
        if (!Boolean.TRUE.equals(body.getSuccess())) {
            throw new BusinessException("ALIYUN_BILLING_QUERY_FAILED", formatBssFailure(body));
        }
        return body;
    }

    private BillingAuditRawLineDO toRawLine(
        BillingAuditRunDO run,
        CloudAccountDO cloudAccount,
        DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyDataItems item,
        String billingCycle,
        int sequence
    ) {
        String rawPayload = Common.toJSONString(Common.toMap(item));
        BillingAuditRawLineDO rawLine = new BillingAuditRawLineDO();
        rawLine.setRunId(run.getId());
        rawLine.setProvider("ALIYUN");
        rawLine.setSourceApi("DescribeInstanceBill");
        rawLine.setSourceBillingCycle(billingCycle);
        rawLine.setBillDate(parseDate(item.getBillingDate(), run.getBillDate()));
        rawLine.setLineHash(hash(sequence + "|" + rawPayload));
        rawLine.setBillAccountId(parseLong(item.getBillAccountID()));
        rawLine.setBillOwnerId(parseLong(item.getOwnerID()));
        rawLine.setPayerAccountId(null);
        rawLine.setProductCode(item.getProductCode());
        rawLine.setProductName(item.getProductName());
        rawLine.setProductDetailCode(item.getProductType());
        rawLine.setProductDetail(item.getProductDetail());
        rawLine.setCommodityCode(item.getCommodityCode());
        rawLine.setCommodityName(null);
        rawLine.setBillingItemCode(item.getBillingItemCode());
        rawLine.setBillingItem(item.getBillingItem());
        rawLine.setBillingType(item.getItem());
        rawLine.setSubscriptionType(item.getSubscriptionType());
        rawLine.setInstanceId(item.getInstanceID());
        rawLine.setInstanceName(firstNotBlank(item.getNickName(), item.getItemName()));
        rawLine.setRegion(item.getRegion());
        rawLine.setZone(item.getZone());
        rawLine.setResourceGroup(item.getResourceGroup());
        rawLine.setCostUnit(item.getCostUnit());
        rawLine.setUsageAmount(parseDecimal(item.getUsage()));
        rawLine.setUsageUnit(item.getUsageUnit());
        rawLine.setPretaxGrossAmount(toDecimal(item.getPretaxGrossAmount()));
        rawLine.setInvoiceDiscount(toDecimal(item.getInvoiceDiscount()));
        rawLine.setDeductibleAmount(null);
        rawLine.setPretaxAmount(toDecimal(item.getPretaxAmount()));
        rawLine.setCashAmount(toDecimal(item.getCashAmount()));
        rawLine.setPaymentAmount(toDecimal(item.getPaymentAmount()));
        rawLine.setOutstandingAmount(toDecimal(item.getOutstandingAmount()));
        rawLine.setCurrency(item.getCurrency());
        rawLine.setTags(item.getTag());
        rawLine.setRawPayload(rawPayload);
        if (rawLine.getBillOwnerId() == null) {
            rawLine.setBillOwnerId(cloudAccount.getBillOwnerId());
        }
        return rawLine;
    }

    private String formatBssFailure(DescribeInstanceBillResponseBody body) {
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

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

    private Long parseLong(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal toDecimal(Float value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(6, RoundingMode.HALF_UP);
    }

    private String firstNotBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        return second;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash billing raw line", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
