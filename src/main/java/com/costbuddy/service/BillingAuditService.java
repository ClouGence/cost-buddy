package com.costbuddy.service;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.BillingAuditItemRuleRequest;
import com.costbuddy.dto.request.BillingAuditRunRequest;
import com.costbuddy.dto.response.BillingAuditItemResourceResponse;
import com.costbuddy.dto.response.MeteredOperationResponse;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingAuditRunMapper;
import com.costbuddy.metering.MeterItemCodes;
import com.costbuddy.metering.UsageMeteringResult;
import com.costbuddy.metering.UsageMeteringService;
import com.motherboard.sdk.model.ResourceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingAuditService {

    private static final String                 RESOURCE_NAME = "billing_audit_run";

    private final CloudAccountService           cloudAccountService;
    private final AliyunBillingRawLineCollector aliyunBillingRawLineCollector;
    private final BillingAuditItemAggregator    billingAuditItemAggregator;
    private final BillingAuditRunMapper         billingAuditRunMapper;
    private final BillingAuditItemMapper        billingAuditItemMapper;
    private final BillingAuditRawLineMapper     billingAuditRawLineMapper;
    private final BillingItemRuleMatcher        billingItemRuleMatcher;
    private final BillingItemRuleService        billingItemRuleService;
    private final CurrentUserProvider           currentUserProvider;
    private final UsageMeteringService          usageMeteringService;

    public BillingAuditService(CloudAccountService cloudAccountService, AliyunBillingRawLineCollector aliyunBillingRawLineCollector,
                               BillingAuditItemAggregator billingAuditItemAggregator, BillingAuditRunMapper billingAuditRunMapper, BillingAuditItemMapper billingAuditItemMapper,
                               BillingAuditRawLineMapper billingAuditRawLineMapper, BillingItemRuleMatcher billingItemRuleMatcher, BillingItemRuleService billingItemRuleService,
                               CurrentUserProvider currentUserProvider, UsageMeteringService usageMeteringService){
        this.cloudAccountService = cloudAccountService;
        this.aliyunBillingRawLineCollector = aliyunBillingRawLineCollector;
        this.billingAuditItemAggregator = billingAuditItemAggregator;
        this.billingAuditRunMapper = billingAuditRunMapper;
        this.billingAuditItemMapper = billingAuditItemMapper;
        this.billingAuditRawLineMapper = billingAuditRawLineMapper;
        this.billingItemRuleMatcher = billingItemRuleMatcher;
        this.billingItemRuleService = billingItemRuleService;
        this.currentUserProvider = currentUserProvider;
        this.usageMeteringService = usageMeteringService;
    }

    public MeteredOperationResponse<BillingAuditRunDO> trigger(BillingAuditRunRequest request) {
        CloudAccountDO cloudAccount = cloudAccountService.get(request.getCloudAccountId());
        UsageMeteringResult metering = usageMeteringService
            .report(MeterItemCodes.AUDIT_RUN, request.getIdempotencyKey(), ResourceType.ALIYUN_CREDENTIAL, cloudAccount.getCredentialResourceId());
        if (!metering.allowed()) {
            return MeteredOperationResponse.rejected(metering);
        }
        BillingAuditRunDO run = new BillingAuditRunDO();
        run.setMotherboardUserId(currentUserProvider.motherboardUserId());
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
            run.setMessage("Collected " + rawLineResult.getRawLineCount() + " bill-date raw lines and aggregated " + aggregationResult.getItemCount() + " audit items.");
        } catch (RuntimeException exception) {
            run.setStatus("FAILED");
            run.setMessage(exception.getMessage());
        }
        run.setFinishedAt(LocalDateTime.now());
        billingAuditRunMapper.update(run);
        return MeteredOperationResponse.allowed(metering, get(run.getId()));
    }

    public BillingAuditRunDO get(Long id) {
        BillingAuditRunDO run = billingAuditRunMapper.selectByIdAndMotherboardUserId(id, currentUserProvider.motherboardUserId());
        if (run == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return run;
    }

    public List<BillingAuditRunDO> list() {
        return billingAuditRunMapper.selectAllByMotherboardUserId(currentUserProvider.motherboardUserId());
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

    @Transactional
    public BillingItemRuleDO createRuleFromItem(Long runId, Long itemId, BillingAuditItemRuleRequest request) {
        if (!"BILLING_ITEM".equals(request.getMatchScope()) || !"IGNORED".equals(request.getDecision())) {
            throw new BusinessException("UNSUPPORTED_AUDIT_ITEM_RULE", "Only BILLING_ITEM ignored rules can be created from audit items");
        }
        BillingAuditItemDO item = getItemInRun(runId, itemId);
        BillingItemRuleDO rule = billingItemRuleService.createFromAuditItem(item, request);
        applyRules(runId);
        return rule;
    }

    @Transactional
    public BillingAuditRunDO applyRules(Long runId) {
        BillingAuditRunDO run = get(runId);
        List<BillingItemRuleDO> rules = billingItemRuleMatcher.sortRules(billingItemRuleService.list());
        BillingAuditAggregationResult result = new BillingAuditAggregationResult();
        for (BillingAuditItemDO item : billingAuditItemMapper.selectByRunId(runId)) {
            String decision = billingItemRuleMatcher.decide(item, rules);
            item.setDecision(decision);
            billingAuditItemMapper.update(item);
            result.addItem(item.getPeriodPretaxAmount(), decision);
        }
        run.setItemCount(result.getItemCount());
        run.setUnknownItemCount(result.getUnknownItemCount());
        run.setTotalPretaxAmount(result.getTotalPretaxAmount());
        run.setUnknownPretaxAmount(result.getUnknownPretaxAmount());
        billingAuditRunMapper.update(run);
        return get(runId);
    }

    private BillingAuditItemDO getItemInRun(Long runId, Long itemId) {
        get(runId);
        BillingAuditItemDO item = billingAuditItemMapper.selectById(itemId);
        if (item == null || !runId.equals(item.getRunId())) {
            throw new NotFoundException("billing_audit_item", itemId);
        }
        return item;
    }
}
