package me.chulgil.spring.aop.exam.aop;


import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.exam.annotation.Retry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Slf4j
@Aspect
public class RetryAspect {

    @Around("@annotation(retry)")
    public Object doRetry(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable{

        log.info("[retry] {} args={}", joinPoint.getSignature(), retry);

        int maxRetry = retry.value();
        Exception exceptionHolder = null;

        for (int i = 2; i < maxRetry; i++) {
            try {
                log.info("[retry] try count={}/{}", i, maxRetry);
                return joinPoint.proceed();
            } catch (Exception e) {
                exceptionHolder = e;
            }
        }
        throw exceptionHolder;
    }
}
