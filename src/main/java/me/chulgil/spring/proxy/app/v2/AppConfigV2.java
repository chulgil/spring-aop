package me.chulgil.spring.proxy.app.v2;

import org.springframework.context.annotation.Bean;

public class AppConfigV2 {

    @Bean
    public OrderController orderControllerV2() {
        return new OrderController(orderServiceV2());
    }

    @Bean
    public OrderService orderServiceV2() {
        return new OrderService(orderRepositoryV2());
    }

    @Bean
    public OrderRepository orderRepositoryV2() {
        return new OrderRepository();
    }

}
