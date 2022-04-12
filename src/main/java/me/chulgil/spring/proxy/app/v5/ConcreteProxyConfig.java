package me.chulgil.spring.proxy.app.v5;

import me.chulgil.spring.proxy.app.v2.OrderController;
import me.chulgil.spring.proxy.app.v2.OrderRepository;
import me.chulgil.spring.proxy.app.v2.OrderService;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConcreteProxyConfig {

    @Bean
    public OrderController orderController(LogTrace logTrace) {
        OrderController controller = new OrderController(orderService(logTrace));
        return new OrderControllerProxy(controller, logTrace);
    }

    @Bean
    public OrderService orderService(LogTrace logTrace) {
        OrderService service = new OrderService(orderRepository(logTrace));
        return new OrderServiceProxy(service, logTrace);
    }

    @Bean
    public OrderRepository orderRepository(LogTrace logTrace) {
        OrderRepository repository = new OrderRepository();
        return new OrderRepositoryProxy(repository, logTrace);
    }
}
