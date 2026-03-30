package com.zhuoyue.platform.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * AOP 日志切面
 * 拦截 Controller 和 Service 层方法，自动打印入参、出参、耗时信息
 */
@Aspect
@Component
public class LogAspect {

    // ===================== 切点定义 =====================

    /** Controller 层切点：拦截 controller 包下所有方法 */
    @Pointcut("execution(* com.zhuoyue.platform.controller..*(..))")
    public void controllerPointcut() {}

    /** Service 层切点：拦截 service.impl 包下所有方法 */
    @Pointcut("execution(* com.zhuoyue.platform.service.impl..*(..))")
    public void servicePointcut() {}

    // ===================== Controller 日志 =====================

    /**
     * 环绕通知：打印 Controller 层请求信息
     * 包含：HTTP方法、URL、类名、方法名、入参、出参、耗时
     */
    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = getLogger(joinPoint);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        // 尝试获取 HTTP 请求信息
        String httpInfo = "";
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                httpInfo = "[" + request.getMethod() + " " + request.getRequestURI() + "] ";
            }
        } catch (Exception ignored) {}

        log.info("→ Controller {}{}  入参: {}", httpInfo, methodName, Arrays.toString(joinPoint.getArgs()));
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("← Controller {}  耗时: {}ms  出参: {}", methodName, System.currentTimeMillis() - start, result);
            return result;
        } catch (Throwable ex) {
            log.error("✕ Controller {}  耗时: {}ms  异常: {}", methodName, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    // ===================== Service 日志 =====================

    /**
     * 环绕通知：打印 Service 层调用信息
     * 包含：类名、方法名、入参、出参、耗时
     */
    @Around("servicePointcut()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = getLogger(joinPoint);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        log.debug("→ Service {}  入参: {}", methodName, Arrays.toString(joinPoint.getArgs()));
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.debug("← Service {}  耗时: {}ms", methodName, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("✕ Service {}  耗时: {}ms  异常: {}", methodName, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    /** 获取目标类的 Logger，日志归属于实际业务类而非切面本身 */
    private Logger getLogger(ProceedingJoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getTarget().getClass());
    }
}
