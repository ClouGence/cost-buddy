package com.costbuddy.common.mybatis;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BaseMapper<T> {

    T selectById(@Param("id") Long id);

    List<T> selectAll();

    int insert(T entity);

    int update(T entity);

    int deleteById(@Param("id") Long id);
}
