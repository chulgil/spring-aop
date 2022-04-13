package me.chulgil.spring.proxy.factory;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.proxy.app.v1.*;
import me.chulgil.spring.proxy.factory.advice.LogTraceAdvice;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
public class ProxyFactoryConfigV1 {

    @Bean
    public IOrderController orderController(LogTrace logTrace) {
        IOrderController orderController = new OrderController(orderService(logTrace));
        ProxyFactory factory = new ProxyFactory(orderController);
        factory.addAdvisor(getAdvisor(logTrace));
        IOrderController proxy = (IOrderController) factory.getProxy();
        log.info("ProxyFactory proxy={}, target={}", proxy.getClass(),
                orderController.getClass());
        return proxy;
    }

    @Bean
    public IOrderService orderService(LogTrace logTrace) {
        IOrderService orderService = new OrderService(orderRepository(logTrace));
        ProxyFactory factory = new ProxyFactory(orderService);
        factory.addAdvisor(getAdvisor(logTrace));
        IOrderService proxy = (IOrderService) factory.getProxy();
        log.info("ProxyFactory proxy={}, target={}", proxy.getClass(),
                orderService.getClass());
        return proxy;
    }

    @Bean
    public IOrderRepository orderRepository(LogTrace logTrace) {
        IOrderRepository orderRepository = new OrderRepository();
        ProxyFactory factory = new ProxyFactory(orderRepository);
        factory.addAdvisor(getAdvisor(logTrace));
        IOrderRepository proxy = (IOrderRepository) factory.getProxy();
        log.info("ProxyFactory proxy={}, target={}", proxy.getClass(), orderRepository.getClass());
        return proxy;
    }

    private Advisor getAdvisor(LogTrace logTrace) {
        //pointcut
        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedNames("request*", "order*", "save*");
        //advice
        LogTraceAdvice advice = new LogTraceAdvice(logTrace);
        //advisor = pointcut + advice
        return new DefaultPointcutAdvisor(pointcut, advice);
    }


}
