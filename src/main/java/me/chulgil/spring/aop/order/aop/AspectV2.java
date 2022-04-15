package me.chulgil.spring.aop.order.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Slf4j
@Aspect
public class AspectV2 {

    /**
     * 포인트컷 시그니처 : 메서드 이름과 파라미터를 합친것으로 주문과 관련된 모든 기능을 대상으로 하는 포인트 컷이다.
     * 반환 타입은 void로 코드의 내용은 비워둬야 한다.
     * 다른 애스팩트에서 참고하려면 public을 사용하고 그렇지 않은경우는 private를 사용한다.
     */
    // 현재 패키지와 하위 패키지
    @Pointcut("execution(* me.chulgil.spring.aop.order..*(..))")
    private void allOrder(){} //pointcut signature

    @Around("allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }

}
