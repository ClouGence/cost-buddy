package com.costbuddy.service;

import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.BillingAuditRunRequest;
import com.costbuddy.dto.response.BillingAuditItemResourceResponse;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingAuditRunMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BillingAuditService {

    private static final String RESOURCE_NAME = "billing_audit_run";

    private final CloudAccountService            cloudAccountService;
    private final AliyunBillingRawLineCollector aliyunBillingRawLineCollector;
    private final BillingAuditItemAggregator     billingAuditItemAggregator;
    private final BillingAuditRunMapper          billingAuditRunMapper;
    private final BillingAuditItemMapper         billingAuditItemMapper;
    private final BillingAuditRawLineMapper      billingAuditRawLineMapper;

    public BillingAuditService(
        CloudAccountService cloudAccountService,
        AliyunBillingRawLineCollector aliyunBillingRawLineCollector,
        BillingAuditItemAggregator billingAuditItemAggregator,
        BillingAuditRunMapper billingAuditRunMapper,
        BillingAuditItemMapper billingAuditItemMapper,
        BillingAuditRawLineMapper billingAuditRawLineMapper
    ) {
        this.cloudAccountService = cloudAccountService;
        this.aliyunBillingRawLineCollector = aliyunBillingRawLineCollector;
        this.billingAuditItemAggregator = billingAuditItemAggregator;
        this.billingAuditRunMapper = billingAuditRunMapper;
        this.billingAuditItemMapper = billingAuditItemMapper;
        this.billingAuditRawLineMapper = billingAuditRawLineMapper;
    }

    public BillingAuditRunDO trigger(BillingAuditRunRequest request) {
        CloudAccountDO cloudAccount = cloudAccountService.get(request.getCloudAccountId());
        BillingAuditRunDO run = new BillingAuditRunDO();
        run.setCloudAccountId(request.getCloudAccountId());
        run.setBillDate(request.getBillDate());
        run.setPeriodStartDate(request.getBillDate());
        run.setPeriodEndDate(request.getBillDate());
        run.setStatus("RUNNING");
        run.setItemCount(0);
        run.setUnknownItemCount(0);
        run.setTotalPretaxAmount(BigDecimal.ZERO);
        run.setUnknownPretaxAmount(BigDecimal.ZERO);
        run.setMessage("Collecting Alibaba Cloud stable-day pay-as-you-go billing item lines.");
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        billingAuditRunMapper.insert(run);
        try {
            BillingRawLineCollectionResult rawLineResult = aliyunBillingRawLineCollector.collectStableDay(run, cloudAccount);
            BillingAuditAggregationResult aggregationResult = billingAuditItemAggregator.aggregate(run);
            run.setStatus("SUCCESS");
            run.setItemCount(aggregationResult.getItemCount());
            run.setUnknownItemCount(aggregationResult.getUnknownItemCount());
            run.setTotalPretaxAmount(aggregationResult.getTotalPretaxAmount());
            run.setUnknownPretaxAmount(aggregationResult.getUnknownPretaxAmount());
            run.setMessage("Collected " + rawLineResult.getRawLineCount() + " bill-date raw lines and aggregated "
                + aggregationResult.getItemCount() + " audit items.");
        } catch (RuntimeException exception) {
            run.setStatus("FAILED");
            run.setMessage(exception.getMessage());
        }
        run.setFinishedAt(LocalDateTime.now());
        billingAuditRunMapper.update(run);
        return get(run.getId());
    }

    public BillingAuditRunDO get(Long id) {
        BillingAuditRunDO run = billingAuditRunMapper.selectById(id);
        if (run == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return run;
    }

    public List<BillingAuditRunDO> list() {
        return billingAuditRunMapper.selectAll();
    }

    public List<BillingAuditItemDO> listItems(Long runId) {
        get(runId);
        return billingAuditItemMapper.selectByRunId(runId);
    }

    public List<BillingAuditItemResourceResponse> listItemResources(Long runId, Long itemId) {
        get(runId);
        BillingAuditItemDO item = billingAuditItemMapper.selectById(itemId);
        if (item == null || !runId.equals(item.getRunId())) {
            throw new NotFoundException("billing_audit_item", itemId);
        }
        return billingAuditRawLineMapper.selectResourcesByAuditItem(item);
    }
}
