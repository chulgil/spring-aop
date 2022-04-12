package me.chulgil.spring.proxy.app.v5;

import me.chulgil.spring.proxy.app.v2.*;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

public class OrderControllerProxy extends OrderController {

    private final OrderController target;
    private final LogTrace logTrace;

    public OrderControllerProxy(OrderController target, LogTrace trace) {
        super(null);
        this.target = target;
        this.logTrace = trace;
    }

    @Override
    public String request(String itemId) {

        TraceStatus status = logTrace.begin("OrderController.request()");
        try {
            String result = target.request(itemId);
            logTrace.end(status);
            return result;
        } catch (Exception e) {
            logTrace.exception(status, e);
            throw e;
        }
    }

}
