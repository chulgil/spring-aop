package me.chulgil.spring.sample.app.v2;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.sample.trace.TraceId;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.hellotrace.HelloTraceV1;
import me.chulgil.spring.sample.trace.hellotrace.HelloTraceV2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final HelloTraceV2 trace;

    public void orderItem(TraceId traceId, String itemId) {
        TraceStatus status = null;
        try {
            status = trace.begin("OrderService.orderItem()");
            orderRepository.save(status.getTraceId(), itemId);
            trace.end(status);
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }
}
