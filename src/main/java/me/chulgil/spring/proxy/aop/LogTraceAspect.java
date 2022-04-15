package me.chulgil.spring.proxy.aop;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Slf4j
@Aspect
public class LogTraceAspect {

    private final LogTrace logTrace;

    public LogTraceAspect(LogTrace logTrace) {
        this.logTrace = logTrace;
    }

    @Around("execution(* me.chulgil.spring.proxy.app..*(..))")
    public Object execute(ProceedingJoinPoint jointPoint) throws Throwable {

        TraceStatus status = null;

        try {
            String message = jointPoint.getSignature().toShortString();
            status = logTrace.begin(message);

            // 로직 호출
            Object result = jointPoint.proceed();

            logTrace.end(status);
            return result;
        } catch (Exception e) {
            logTrace.exception(status, e);
            throw e;
        }
    }
}
