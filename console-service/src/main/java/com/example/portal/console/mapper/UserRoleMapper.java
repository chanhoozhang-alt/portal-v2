package com.example.portal.console.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/** 用户角色分配 Mapper。 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
