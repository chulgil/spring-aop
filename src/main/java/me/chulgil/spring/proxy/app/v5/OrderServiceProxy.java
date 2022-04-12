package me.chulgil.spring.proxy.app.v5;

import me.chulgil.spring.proxy.app.v2.OrderService;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

public class OrderServiceProxy extends OrderService {

    private final OrderService target;
    private final LogTrace logTrace;

    public OrderServiceProxy(OrderService target, LogTrace logTrace) {
        super(null);
        this.target = target;
        this.logTrace = logTrace;
    }

    @Override
    public void orderItem(String itemId) {

        TraceStatus status = null;
        try {
            status = logTrace.begin("OrderService.orderItem()");
            target.orderItem(itemId);
            logTrace.end(status);
        } catch (Exception e) {
            logTrace.exception(status, e);
            throw e;
        }
    }
}
