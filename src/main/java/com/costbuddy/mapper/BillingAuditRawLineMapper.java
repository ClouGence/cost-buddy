package com.costbuddy.mapper;

import com.costbuddy.common.mybatis.BaseMapper;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRawLineDO;
import com.costbuddy.dto.response.BillingAuditItemResourceResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingAuditRawLineMapper extends BaseMapper<BillingAuditRawLineDO> {

    List<BillingAuditRawLineDO> selectByRunId(@Param("runId") Long runId);

    List<BillingAuditItemResourceResponse> selectResourcesByAuditItem(@Param("item") BillingAuditItemDO item);

    int deleteByRunId(@Param("runId") Long runId);
}
