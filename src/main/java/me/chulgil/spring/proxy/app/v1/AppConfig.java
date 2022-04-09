package me.chulgil.spring.proxy.app.v1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public IOrderController orderController() {
        return new OrderController(orderService());
    }

    @Bean
    public IOrderService orderService() {
        return new OrderService(orderRepository());
    }

    @Bean
    public IOrderRepository orderRepository() {
        return new OrderRepository();
    }
}
