package com.example.portal.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 用户角色分配 Mapper（server 模块），含自定义查询方法。 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    List<UserRole> selectByAppAndUser(@Param("appCode") String appCode, @Param("userId") String userId);

    int countEnabledByAppAndUser(@Param("appCode") String appCode, @Param("userId") String userId);
}
