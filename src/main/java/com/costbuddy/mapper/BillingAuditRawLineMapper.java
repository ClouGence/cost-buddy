package com.costbuddy.mapper;

import com.costbuddy.common.mybatis.BaseMapper;
import com.costbuddy.domain.BillingAuditRawLineDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingAuditRawLineMapper extends BaseMapper<BillingAuditRawLineDO> {

    List<BillingAuditRawLineDO> selectByRunId(@Param("runId") Long runId);

    int deleteByRunId(@Param("runId") Long runId);
}
