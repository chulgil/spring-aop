package me.chulgil.spring.proxy.app.v4;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.proxy.app.v1.IOrderController;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;

@RequiredArgsConstructor
public class OrderControllerProxy implements IOrderController {

    private final IOrderController target;
    private final LogTrace logTrace;

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

    @Override
    public String noLog() {
        return target.noLog();
    }
}
