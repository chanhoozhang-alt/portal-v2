package com.example.portal.console.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解，标注在需要记录操作日志的接口方法上。
 * 由 OperationLogAspect 切面拦截处理。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String module() default "";

    String type() default "";

    String desc() default "";
}
