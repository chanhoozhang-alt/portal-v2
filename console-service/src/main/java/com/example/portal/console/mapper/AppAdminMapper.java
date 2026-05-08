package com.example.portal.console.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.AppAdmin;
import org.apache.ibatis.annotations.Mapper;

/** 应用管理员 Mapper。 */
@Mapper
public interface AppAdminMapper extends BaseMapper<AppAdmin> {
}
