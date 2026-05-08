package com.example.portal.console.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.AppInfo;
import org.apache.ibatis.annotations.Mapper;

/** 应用信息 Mapper（console 模块）。 */
@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {
}
