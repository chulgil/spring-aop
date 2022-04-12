package me.chulgil.spring.proxy.app.v5;

import me.chulgil.spring.proxy.app.v2.OrderRepository;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

public class OrderRepositoryProxy extends OrderRepository {

    private final OrderRepository target;
    private final LogTrace logTrace;

    public OrderRepositoryProxy(OrderRepository target, LogTrace logTrace) {
        this.target = target;
        this.logTrace = logTrace;
    }

    @Override
    public void save(String itemId) {

        TraceStatus status = null;
        try {
            status = logTrace.begin("OrderRepository.request()");
            target.save(itemId);
            logTrace.end(status);
        } catch (Exception e) {
            logTrace.exception(status, e);
            throw e;
        }
    }

}
