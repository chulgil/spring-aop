package me.chulgil.spring.aop.order.aop;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {

    // 현재 패키지와 하위 패키지
    @Pointcut("execution(* me.chulgil.spring.aop.order..*(..))")
    public void allOrder(){}

    // 타입 패턴이 *Service
    @Pointcut("execution(* *..*Service.*(..))")
    public void allService(){}

    // allOrder && allService
    @Pointcut("allOrder() && allService()")
    public void orderAndService(){}
    
}
