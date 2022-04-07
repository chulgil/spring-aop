package me.chulgil.spring.sample.app.v1;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.hellotrace.HelloTraceV1;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final HelloTraceV1 trace;

    public void orderItem(String itemId) {
        TraceStatus status = null;
        try {
            status = trace.begin("OrderService.orderItem()");
            orderRepository.save(itemId);
            trace.end(status);
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }
}
