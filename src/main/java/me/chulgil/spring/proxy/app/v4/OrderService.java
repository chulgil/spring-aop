package me.chulgil.spring.proxy.app.v4;

import me.chulgil.spring.proxy.app.v1.IOrderRepository;
import me.chulgil.spring.proxy.app.v1.IOrderService;

public class OrderService implements IOrderService {

    private final me.chulgil.spring.proxy.app.v1.IOrderRepository orderRepository;

    public OrderService(IOrderRepository repository) {
        this.orderRepository = repository;
    }

    @Override
    public void orderItem(String itemId) {
        orderRepository.save(itemId);
    }
}
