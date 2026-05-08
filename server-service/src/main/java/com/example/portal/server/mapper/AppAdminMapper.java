package com.example.portal.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.AppAdmin;
import org.apache.ibatis.annotations.Mapper;

/** 应用管理员 Mapper（server 模块）。 */
@Mapper
public interface AppAdminMapper extends BaseMapper<AppAdmin> {
}
