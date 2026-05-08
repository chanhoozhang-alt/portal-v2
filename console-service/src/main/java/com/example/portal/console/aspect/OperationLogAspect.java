package com.example.portal.console.aspect;

import com.example.portal.common.context.UserContext;
import com.example.portal.common.model.entity.OperationLog;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.console.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 操作日志切面，拦截 @OperationLog 注解的接口，记录操作人、URI、结果状态到数据库。
 * 使用 finally 确保日志入库，日志写入失败不影响主业务流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, com.example.portal.console.aspect.OperationLog operationLog) throws Throwable {
        UserContext ctx = UserContext.get();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;

        OperationLog logEntity = new OperationLog();
        logEntity.setModuleName(operationLog.module());
        logEntity.setOperationType(operationLog.type());
        logEntity.setOperationDesc(operationLog.desc());
        logEntity.setOperatorId(ctx != null ? ctx.getUserId() : "unknown");
        logEntity.setOperatorName(ctx != null ? ctx.getUserName() : "unknown");
        logEntity.setRequestUri(request != null ? request.getRequestURI() : "");
        logEntity.setRequestMethod(request != null ? request.getMethod() : "");
        logEntity.setOperationTime(new Date());

        try {
            Object result = joinPoint.proceed();
            logEntity.setResultStatus(CommonConstant.RESULT_SUCCESS);
            return result;
        } catch (Throwable e) {
            logEntity.setResultStatus(CommonConstant.RESULT_FAIL);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                operationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }
}
