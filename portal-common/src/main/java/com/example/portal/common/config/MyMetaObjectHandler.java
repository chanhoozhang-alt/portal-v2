package com.example.portal.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.portal.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis-Plus 字段自动填充处理器。
 * 配合实体类 {@code @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)} 使用，
 * 自动填充创建人、创建时间、更新人、更新时间等审计字段。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_BY = "createBy";
    private static final String CREATE_NAME = "createName";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_BY = "updateBy";
    private static final String UPDATE_NAME = "updateName";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        strictInsertFill(metaObject, CREATE_TIME, Date.class, now);
        strictInsertFill(metaObject, UPDATE_TIME, Date.class, now);

        // 从请求上下文取操作人信息（可能为 null，如内部定时任务）
        UserContext ctx = UserContext.get();
        if (ctx != null) {
            strictInsertFill(metaObject, CREATE_BY, String.class, ctx.getUserId());
            strictInsertFill(metaObject, CREATE_NAME, String.class, ctx.getUserName());
            strictInsertFill(metaObject, UPDATE_BY, String.class, ctx.getUserId());
            strictInsertFill(metaObject, UPDATE_NAME, String.class, ctx.getUserName());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, UPDATE_TIME, Date.class, new Date());

        UserContext ctx = UserContext.get();
        if (ctx != null) {
            strictUpdateFill(metaObject, UPDATE_BY, String.class, ctx.getUserId());
            strictUpdateFill(metaObject, UPDATE_NAME, String.class, ctx.getUserName());
        }
    }
}
