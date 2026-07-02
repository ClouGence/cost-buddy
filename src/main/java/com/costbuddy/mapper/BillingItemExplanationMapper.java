package com.costbuddy.mapper;

import com.costbuddy.common.mybatis.BaseMapper;
import com.costbuddy.domain.BillingItemExplanationDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingItemExplanationMapper extends BaseMapper<BillingItemExplanationDO> {

    List<BillingItemExplanationDO> selectByAuditItemId(@Param("auditItemId") Long auditItemId);
}
