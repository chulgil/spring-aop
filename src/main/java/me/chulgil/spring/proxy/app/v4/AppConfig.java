package me.chulgil.spring.proxy.app.v4;

import me.chulgil.spring.proxy.app.v1.IOrderController;
import me.chulgil.spring.proxy.app.v1.IOrderRepository;
import me.chulgil.spring.proxy.app.v1.IOrderService;
import me.chulgil.spring.proxy.app.v1.OrderController;
import me.chulgil.spring.proxy.app.v1.OrderRepository;
import me.chulgil.spring.proxy.app.v1.OrderService;
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
