package com.example.portal.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.portal.common.model.entity.AppInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 应用信息 Mapper（server 模块），含可见应用查询。 */
@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {

    List<AppInfo> selectVisibleApps(@Param("userId") String userId);
}
