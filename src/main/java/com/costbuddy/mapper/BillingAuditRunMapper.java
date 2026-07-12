package com.costbuddy.mapper;

import com.costbuddy.domain.BillingAuditRunDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BillingAuditRunMapper {

    BillingAuditRunDO selectByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);

    List<BillingAuditRunDO> selectAllByMotherboardUserId(@Param("motherboardUserId") Long motherboardUserId);

    int insert(BillingAuditRunDO entity);

    int update(BillingAuditRunDO entity);

    int deleteByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);
}
