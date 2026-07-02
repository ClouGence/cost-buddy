package com.costbuddy.service;

import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRawLineDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingItemRuleMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingAuditItemAggregator {

    private final BillingAuditRawLineMapper billingAuditRawLineMapper;
    private final BillingAuditItemMapper    billingAuditItemMapper;
    private final BillingItemRuleMapper     billingItemRuleMapper;

    public BillingAuditItemAggregator(
        BillingAuditRawLineMapper billingAuditRawLineMapper,
        BillingAuditItemMapper billingAuditItemMapper,
        BillingItemRuleMapper billingItemRuleMapper
    ) {
        this.billingAuditRawLineMapper = billingAuditRawLineMapper;
        this.billingAuditItemMapper = billingAuditItemMapper;
        this.billingItemRuleMapper = billingItemRuleMapper;
    }

    @Transactional
    public BillingAuditAggregationResult aggregate(BillingAuditRunDO run) {
        billingAuditItemMapper.deleteByRunId(run.getId());
        List<BillingItemRuleDO> rules = sortRules(billingItemRuleMapper.selectAll());
        Map<AggregationKey, ItemAccumulator> accumulators = new LinkedHashMap<>();
        for (BillingAuditRawLineDO rawLine : billingAuditRawLineMapper.selectByRunId(run.getId())) {
            AggregationKey key = AggregationKey.from(rawLine);
            accumulators.computeIfAbsent(key, ignored -> new ItemAccumulator(key)).add(rawLine);
        }

        BillingAuditAggregationResult result = new BillingAuditAggregationResult();
        accumulators.values().stream()
            .map(accumulator -> accumulator.toAuditItem(run.getId(), rules))
            .sorted(Comparator.comparing(BillingAuditItemDO::getPeriodPretaxAmount, Comparator.reverseOrder()))
            .forEach(item -> {
                billingAuditItemMapper.insert(item);
                result.addItem(item.getPeriodPretaxAmount(), item.getDecision());
            });
        return result;
    }

    private List<BillingItemRuleDO> sortRules(List<BillingItemRuleDO> rules) {
        List<BillingItemRuleDO> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator
            .comparingInt((BillingItemRuleDO rule) -> "BILLING_ITEM".equals(rule.getMatchScope()) ? 0 : 1)
            .thenComparing(rule -> rule.getId() == null ? 0L : -rule.getId()));
        return sortedRules;
    }

    private String decide(BillingAuditItemDO item, List<BillingItemRuleDO> rules) {
        for (BillingItemRuleDO rule : rules) {
            if (matches(rule, item)) {
                return isBlank(rule.getDecision()) ? "UNKNOWN" : rule.getDecision();
            }
        }
        return "UNKNOWN";
    }

    private boolean matches(BillingItemRuleDO rule, BillingAuditItemDO item) {
        if (!equalsValue(rule.getProvider(), item.getProvider())) {
            return false;
        }
        if ("BILLING_ITEM".equals(rule.getMatchScope())) {
            return hasAnyBillingItemRuleField(rule)
                && fieldMatches(rule.getProductCode(), item.getProductCode())
                && fieldMatches(rule.getProductName(), item.getProductName())
                && fieldMatches(rule.getProductDetail(), item.getProductDetail())
                && fieldMatches(rule.getCommodityCode(), item.getCommodityCode())
                && fieldMatches(rule.getBillingItemCode(), item.getBillingItemCode())
                && fieldMatches(rule.getBillingItem(), item.getBillingItem());
        }
        return hasAnyProductRuleField(rule)
            && fieldMatches(rule.getProductCode(), item.getProductCode())
            && fieldMatches(rule.getProductName(), item.getProductName())
            && fieldMatches(rule.getProductDetail(), item.getProductDetail())
            && fieldMatches(rule.getCommodityCode(), item.getCommodityCode());
    }

    private boolean hasAnyBillingItemRuleField(BillingItemRuleDO rule) {
        return hasAnyProductRuleField(rule) || !isBlank(rule.getBillingItemCode()) || !isBlank(rule.getBillingItem());
    }

    private boolean hasAnyProductRuleField(BillingItemRuleDO rule) {
        return !isBlank(rule.getProductCode())
            || !isBlank(rule.getProductName())
            || !isBlank(rule.getProductDetail())
            || !isBlank(rule.getCommodityCode());
    }

    private boolean fieldMatches(String ruleValue, String itemValue) {
        return isBlank(ruleValue) || equalsValue(ruleValue, itemValue);
    }

    private boolean equalsValue(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.trim().equals(second.trim());
    }

    private String firstNotBlank(String current, String candidate) {
        if (!isBlank(current)) {
            return current;
        }
        return candidate;
    }

    private String decimalText(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AggregationKey(
        String provider,
        String productCode,
        String productName,
        String productDetail,
        String commodityCode,
        String billingItemCode,
        String billingItem,
        String subscriptionType,
        String currency
    ) {

        private static AggregationKey from(BillingAuditRawLineDO rawLine) {
            return new AggregationKey(
                rawLine.getProvider(),
                rawLine.getProductCode(),
                rawLine.getProductName(),
                rawLine.getProductDetail(),
                rawLine.getCommodityCode(),
                rawLine.getBillingItemCode(),
                rawLine.getBillingItem(),
                rawLine.getSubscriptionType(),
                rawLine.getCurrency()
            );
        }
    }

    private class ItemAccumulator {

        private final AggregationKey key;
        private final Set<String>    instanceIds = new LinkedHashSet<>();
        private final Set<String>    regions = new LinkedHashSet<>();
        private final Set<String>    billingTypes = new LinkedHashSet<>();
        private BigDecimal           stableDayPretaxAmount = BigDecimal.ZERO;
        private String               sampleInstanceId;
        private String               sampleRegion;
        private String               sampleUsage;
        private String               sampleUsageUnit;

        private ItemAccumulator(AggregationKey key) {
            this.key = key;
        }

        private void add(BillingAuditRawLineDO rawLine) {
            if (rawLine.getPretaxAmount() != null) {
                stableDayPretaxAmount = stableDayPretaxAmount.add(rawLine.getPretaxAmount());
            }
            if (!isBlank(rawLine.getInstanceId())) {
                instanceIds.add(rawLine.getInstanceId());
            }
            if (!isBlank(rawLine.getRegion())) {
                regions.add(rawLine.getRegion());
            }
            if (!isBlank(rawLine.getBillingType())) {
                billingTypes.add(rawLine.getBillingType());
            }
            sampleInstanceId = firstNotBlank(sampleInstanceId, rawLine.getInstanceId());
            sampleRegion = firstNotBlank(sampleRegion, rawLine.getRegion());
            sampleUsage = firstNotBlank(sampleUsage, decimalText(rawLine.getUsageAmount()));
            sampleUsageUnit = firstNotBlank(sampleUsageUnit, rawLine.getUsageUnit());
        }

        private BillingAuditItemDO toAuditItem(Long runId, List<BillingItemRuleDO> rules) {
            BillingAuditItemDO item = new BillingAuditItemDO();
            item.setRunId(runId);
            item.setProvider(key.provider());
            item.setProductCode(key.productCode());
            item.setProductName(key.productName());
            item.setProductDetail(key.productDetail());
            item.setCommodityCode(key.commodityCode());
            item.setBillingItemCode(key.billingItemCode());
            item.setBillingItem(key.billingItem());
            item.setBillingType(compactType(billingTypes));
            item.setSubscriptionType(key.subscriptionType());
            item.setCurrency(key.currency());
            item.setStableDayPretaxAmount(stableDayPretaxAmount.setScale(6, RoundingMode.HALF_UP));
            item.setPeriodPretaxAmount(stableDayPretaxAmount.setScale(6, RoundingMode.HALF_UP));
            item.setInstanceCount(instanceIds.size());
            item.setRegionCount(regions.size());
            item.setSampleInstanceId(sampleInstanceId);
            item.setSampleRegion(sampleRegion);
            item.setSampleUsage(sampleUsage);
            item.setSampleUsageUnit(sampleUsageUnit);
            item.setDecision(decide(item, rules));
            return item;
        }

        private String compactType(Set<String> values) {
            if (values.isEmpty()) {
                return null;
            }
            if (values.size() == 1) {
                return values.iterator().next();
            }
            return "MIXED";
        }
    }
}
