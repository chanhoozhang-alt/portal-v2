package com.example.portal.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.SystemAdmin;
import org.apache.ibatis.annotations.Mapper;

/** 系统管理员 Mapper（server 模块）。 */
@Mapper
public interface SystemAdminMapper extends BaseMapper<SystemAdmin> {
}
