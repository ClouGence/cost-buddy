package com.costbuddy.mapper;

import com.costbuddy.domain.CloudAccountDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CloudAccountMapper {

    CloudAccountDO selectByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);

    List<CloudAccountDO> selectAllByMotherboardUserId(@Param("motherboardUserId") Long motherboardUserId);

    int insert(CloudAccountDO entity);

    int update(CloudAccountDO entity);

    int deleteByIdAndMotherboardUserId(@Param("id") Long id, @Param("motherboardUserId") Long motherboardUserId);
}
