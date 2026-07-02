package com.costbuddy.mapper;

import com.costbuddy.common.mybatis.BaseMapper;
import com.costbuddy.domain.BillingAuditItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingAuditItemMapper extends BaseMapper<BillingAuditItemDO> {

    List<BillingAuditItemDO> selectByRunId(@Param("runId") Long runId);

    int deleteByRunId(@Param("runId") Long runId);
}
