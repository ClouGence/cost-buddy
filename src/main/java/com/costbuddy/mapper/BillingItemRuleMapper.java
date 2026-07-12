package com.costbuddy.mapper;

import com.costbuddy.domain.BillingItemRuleDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingItemRuleMapper {

    BillingItemRuleDO selectByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);

    List<BillingItemRuleDO> selectAllByMotherboardUserId(@Param("motherboardUserId") Long motherboardUserId);

    int insert(BillingItemRuleDO entity);

    int update(BillingItemRuleDO entity);

    int deleteByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);
}
