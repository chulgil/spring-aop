package me.chulgil.spring.proxy.app.v4;

import me.chulgil.spring.proxy.app.v1.*;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class InterfaceProxyConfig {

    @Bean
    public IOrderController orderController(LogTrace logTrace) {
        OrderController controller = new OrderController(orderService(logTrace));
        return new OrderControllerProxy(controller, logTrace);
    }

    @Bean
    public IOrderService orderService(LogTrace logTrace) {
        OrderService service = new OrderService(orderRepository(logTrace));
        return new OrderServiceProxy(service, logTrace);
    }

    @Bean
    public IOrderRepository orderRepository(LogTrace logTrace) {
        OrderRepository repository = new OrderRepository();
        return new OrderRepositoryProxy(repository, logTrace);
    }
}
