package com.costbuddy.service;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.dto.request.BillingAuditItemRuleRequest;
import com.costbuddy.dto.request.BillingItemRuleRequest;
import com.costbuddy.mapper.BillingItemRuleMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingItemRuleService {

    private static final String         RESOURCE_NAME = "billing_item_rule";

    private final BillingItemRuleMapper billingItemRuleMapper;
    private final CurrentUserProvider   currentUserProvider;

    public BillingItemRuleService(BillingItemRuleMapper billingItemRuleMapper, CurrentUserProvider currentUserProvider){
        this.billingItemRuleMapper = billingItemRuleMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public BillingItemRuleDO create(BillingItemRuleRequest request) {
        BillingItemRuleDO rule = new BillingItemRuleDO();
        BeanUtils.copyProperties(request, rule);
        rule.setMotherboardUserId(currentUserProvider.motherboardUserId());
        normalize(rule);
        billingItemRuleMapper.insert(rule);
        return get(rule.getId());
    }

    public BillingItemRuleDO get(Long id) {
        BillingItemRuleDO rule = billingItemRuleMapper.selectByIdAndMotherboardUserId(id, currentUserProvider.motherboardUserId());
        if (rule == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return rule;
    }

    public List<BillingItemRuleDO> list() {
        return billingItemRuleMapper.selectAllByMotherboardUserId(currentUserProvider.motherboardUserId());
    }

    @Transactional
    public BillingItemRuleDO update(Long id, BillingItemRuleRequest request) {
        get(id);
        BillingItemRuleDO rule = new BillingItemRuleDO();
        BeanUtils.copyProperties(request, rule);
        rule.setId(id);
        rule.setMotherboardUserId(currentUserProvider.motherboardUserId());
        normalize(rule);
        billingItemRuleMapper.update(rule);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        billingItemRuleMapper.deleteByIdAndMotherboardUserId(id, currentUserProvider.motherboardUserId());
    }

    @Transactional
    public BillingItemRuleDO createFromAuditItem(BillingAuditItemDO item, BillingAuditItemRuleRequest request) {
        BillingItemRuleDO rule = new BillingItemRuleDO();
        rule.setMotherboardUserId(currentUserProvider.motherboardUserId());
        rule.setProvider(item.getProvider());
        rule.setMatchScope(request.getMatchScope());
        rule.setProductCode(item.getProductCode());
        rule.setProductName(item.getProductName());
        rule.setProductDetail(item.getProductDetail());
        rule.setCommodityCode(item.getCommodityCode());
        if ("BILLING_ITEM".equals(request.getMatchScope())) {
            rule.setBillingItemCode(item.getBillingItemCode());
            rule.setBillingItem(item.getBillingItem());
        }
        rule.setDecision(request.getDecision());
        rule.setNote(request.getNote());
        normalize(rule);
        billingItemRuleMapper.insert(rule);
        return get(rule.getId());
    }

    private void normalize(BillingItemRuleDO rule) {
        rule.setProvider(clean(rule.getProvider()));
        rule.setMatchScope(clean(rule.getMatchScope()));
        rule.setProductCode(clean(rule.getProductCode()));
        rule.setProductName(clean(rule.getProductName()));
        rule.setProductDetail(clean(rule.getProductDetail()));
        rule.setCommodityCode(clean(rule.getCommodityCode()));
        rule.setBillingItemCode(clean(rule.getBillingItemCode()));
        rule.setBillingItem(clean(rule.getBillingItem()));
        rule.setDecision(clean(rule.getDecision()));
        rule.setNote(clean(rule.getNote()));
        if (rule.getProvider() == null) {
            rule.setProvider("ALIYUN");
        }
        if (!"PRODUCT".equals(rule.getMatchScope()) && !"BILLING_ITEM".equals(rule.getMatchScope())) {
            throw new BusinessException("INVALID_RULE_SCOPE", "matchScope must be PRODUCT or BILLING_ITEM");
        }
        if (!"KNOWN".equals(rule.getDecision()) && !"IGNORED".equals(rule.getDecision())) {
            throw new BusinessException("INVALID_RULE_DECISION", "decision must be KNOWN or IGNORED");
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
