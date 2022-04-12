package me.chulgil.spring.proxy.app.v4;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.proxy.app.v1.IOrderService;
import me.chulgil.spring.proxy.app.v1.OrderService;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

@RequiredArgsConstructor
public class OrderServiceProxy implements IOrderService {

    private final OrderService target;
    private final LogTrace logTrace;

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
