package me.chulgil.spring.proxy.app.v1;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderController implements IOrderController {

    private final IOrderService orderService;

    public OrderController(IOrderService service) {
        this.orderService = service;
    }


    @Override
    public String request(String itemId) {
        orderService.orderItem(itemId);
        return "ok";
    }

    @Override
    public String noLog() {
        return "ok";
    }
}
