package me.chulgil.spring.proxy.dynamic;

import me.chulgil.spring.proxy.app.v1.*;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Proxy;

public class DynamicProxyFilterConfig {

    public static final String[] PATTERNS = {"request*", "order*", "save*"};

    @Bean
    public IOrderController controller(LogTrace logTrace) {
        IOrderController proxy = (IOrderController) Proxy.newProxyInstance(
                DynamicProxyFilterConfig.class.getClassLoader(),
                new Class[]{IOrderController.class},
                new LogTraceFilterHandler(new OrderController(service(logTrace)), logTrace, PATTERNS)
        );
        return proxy;
    }

    @Bean
    public IOrderService service(LogTrace logTrace) {
        IOrderService proxy = (IOrderService) Proxy.newProxyInstance(
                DynamicProxyFilterConfig.class.getClassLoader(),
                new Class[]{IOrderService.class},
                new LogTraceFilterHandler(new OrderService(repository(logTrace)), logTrace, PATTERNS)
        );
        return proxy;
    }

    @Bean
    public IOrderRepository repository(LogTrace logTrace) {
        IOrderRepository proxy = (IOrderRepository) Proxy.newProxyInstance(
                DynamicProxyFilterConfig.class.getClassLoader(),
                new Class[]{IOrderRepository.class},
                new LogTraceFilterHandler(new OrderRepository(), logTrace, PATTERNS)
        );
        return proxy;
    }
}