package com.example.portal.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.BizAdmin;
import org.apache.ibatis.annotations.Mapper;

/** 业务管理员 Mapper（server 模块）。 */
@Mapper
public interface BizAdminMapper extends BaseMapper<BizAdmin> {
}
