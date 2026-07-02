package com.costbuddy.service;

import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingItemRuleDO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BillingItemRuleMatcher {

    public List<BillingItemRuleDO> sortRules(List<BillingItemRuleDO> rules) {
        List<BillingItemRuleDO> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator
            .comparingInt((BillingItemRuleDO rule) -> "BILLING_ITEM".equals(rule.getMatchScope()) ? 0 : 1)
            .thenComparing(rule -> rule.getId() == null ? 0L : -rule.getId()));
        return sortedRules;
    }

    public String decide(BillingAuditItemDO item, List<BillingItemRuleDO> sortedRules) {
        for (BillingItemRuleDO rule : sortedRules) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
