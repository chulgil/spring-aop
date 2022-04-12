package me.chulgil.spring.proxy.dynamic;

import me.chulgil.spring.proxy.app.v1.IOrderController;
import me.chulgil.spring.proxy.app.v1.IOrderRepository;
import me.chulgil.spring.proxy.app.v1.IOrderService;
import me.chulgil.spring.proxy.app.v1.OrderController;
import me.chulgil.spring.proxy.app.v1.OrderRepository;
import me.chulgil.spring.proxy.app.v1.OrderService;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

public class DynamicProxyBasicConfig {

    @Bean
    public IOrderController orderController(LogTrace logTrace) {
        return (IOrderController) Proxy.newProxyInstance(
                IOrderController.class.getClassLoader(),
                new Class[]{IOrderController.class},
                new LogTraceBasicHandler(new OrderController(orderService(logTrace)), logTrace)
        );
    }

    @Bean
    public IOrderService orderService(LogTrace logTrace) {
        return (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class},
                new LogTraceBasicHandler(new OrderService(orderRepository(logTrace)), logTrace)
        );
    }

    @Bean
    public IOrderRepository orderRepository(LogTrace logTrace) {
        return (IOrderRepository) Proxy.newProxyInstance(
                IOrderRepository.class.getClassLoader(),
                new Class[]{IOrderRepository.class},
                new LogTraceBasicHandler(new OrderRepository(), logTrace)
        );
    }
}
