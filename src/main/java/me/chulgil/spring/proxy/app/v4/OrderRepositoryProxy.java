package me.chulgil.spring.proxy.app.v4;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.proxy.app.v1.IOrderRepository;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

@RequiredArgsConstructor
public class OrderRepositoryProxy implements IOrderRepository {

    private final IOrderRepository target;
    private final LogTrace logTrace;

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
