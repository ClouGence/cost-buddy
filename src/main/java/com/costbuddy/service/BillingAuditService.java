package com.costbuddy.service;

import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.BillingAuditRunRequest;
import com.costbuddy.mapper.BillingAuditItemMapper;
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
    private final BillingAuditRunMapper          billingAuditRunMapper;
    private final BillingAuditItemMapper         billingAuditItemMapper;

    public BillingAuditService(
        CloudAccountService cloudAccountService,
        AliyunBillingRawLineCollector aliyunBillingRawLineCollector,
        BillingAuditRunMapper billingAuditRunMapper,
        BillingAuditItemMapper billingAuditItemMapper
    ) {
        this.cloudAccountService = cloudAccountService;
        this.aliyunBillingRawLineCollector = aliyunBillingRawLineCollector;
        this.billingAuditRunMapper = billingAuditRunMapper;
        this.billingAuditItemMapper = billingAuditItemMapper;
    }

    public BillingAuditRunDO trigger(BillingAuditRunRequest request) {
        if (request.getPeriodEndDate().isBefore(request.getPeriodStartDate())) {
            throw new BusinessException("INVALID_AUDIT_WINDOW", "periodEndDate must not be before periodStartDate");
        }
        CloudAccountDO cloudAccount = cloudAccountService.get(request.getCloudAccountId());
        BillingAuditRunDO run = new BillingAuditRunDO();
        run.setCloudAccountId(request.getCloudAccountId());
        run.setBillDate(request.getBillDate());
        run.setPeriodStartDate(request.getPeriodStartDate());
        run.setPeriodEndDate(request.getPeriodEndDate());
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
            BillingRawLineCollectionResult result = aliyunBillingRawLineCollector.collectStableDay(run, cloudAccount);
            run.setStatus("SUCCESS");
            run.setItemCount(result.getRawLineCount());
            run.setUnknownItemCount(0);
            run.setTotalPretaxAmount(result.getTotalPretaxAmount());
            run.setUnknownPretaxAmount(BigDecimal.ZERO);
            run.setMessage("Collected " + result.getRawLineCount() + " raw billing lines from DescribeInstanceBill.");
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
}
