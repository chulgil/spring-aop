package me.chulgil.spring.proxy.app.v4;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.proxy.app.v1.IOrderController;
import me.chulgil.spring.proxy.app.v1.IOrderService;

@Slf4j
public class OrderController implements IOrderController {

    private final me.chulgil.spring.proxy.app.v1.IOrderService orderService;

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
