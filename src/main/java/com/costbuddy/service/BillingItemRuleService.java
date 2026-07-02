package com.costbuddy.service;

import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.BillingItemRuleDO;
import com.costbuddy.dto.request.BillingItemRuleRequest;
import com.costbuddy.mapper.BillingItemRuleMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingItemRuleService {

    private static final String RESOURCE_NAME = "billing_item_rule";

    private final BillingItemRuleMapper billingItemRuleMapper;

    public BillingItemRuleService(BillingItemRuleMapper billingItemRuleMapper) {
        this.billingItemRuleMapper = billingItemRuleMapper;
    }

    @Transactional
    public BillingItemRuleDO create(BillingItemRuleRequest request) {
        BillingItemRuleDO rule = new BillingItemRuleDO();
        BeanUtils.copyProperties(request, rule);
        normalize(rule);
        billingItemRuleMapper.insert(rule);
        return get(rule.getId());
    }

    public BillingItemRuleDO get(Long id) {
        BillingItemRuleDO rule = billingItemRuleMapper.selectById(id);
        if (rule == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return rule;
    }

    public List<BillingItemRuleDO> list() {
        return billingItemRuleMapper.selectAll();
    }

    @Transactional
    public BillingItemRuleDO update(Long id, BillingItemRuleRequest request) {
        get(id);
        BillingItemRuleDO rule = new BillingItemRuleDO();
        BeanUtils.copyProperties(request, rule);
        rule.setId(id);
        normalize(rule);
        billingItemRuleMapper.update(rule);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        billingItemRuleMapper.deleteById(id);
    }

    private void normalize(BillingItemRuleDO rule) {
        if (rule.getProvider() == null || rule.getProvider().isBlank()) {
            rule.setProvider("ALIYUN");
        }
    }
}
