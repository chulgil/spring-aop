package me.chulgil.spring.sample.app.v5;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.sample.trace.callback.TraceTemplate;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TraceTemplate template;

    public OrderService(OrderRepository orderRepository, LogTrace trace) {
        this.orderRepository = orderRepository;
        this.template = new TraceTemplate(trace);
    }

    public void orderItem(String itemId) {

        template.execute("OrderController.request()", () ->  {
            orderRepository.save(itemId);
            return null;
        });
    }
}
