package me.chulgil.spring.proxy.app.v3;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository repository) {
        this.orderRepository = repository;
    }

    public void orderItem(String itemId) {
        orderRepository.save(itemId);
    }
}
