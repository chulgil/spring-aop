package me.chulgil.spring.sample.app.v5;

import me.chulgil.spring.sample.trace.callback.TraceCallback;
import me.chulgil.spring.sample.trace.callback.TraceTemplate;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final TraceTemplate template;

    public OrderController(OrderService service, LogTrace trace) {
        this.orderService = service;
        this.template = new TraceTemplate(trace);
    }

    @GetMapping("/v5/request")
    public String request(String itemId) {
        return template.execute("OrderController.request()", new TraceCallback<>() {
            @Override
            public String call() {
                orderService.orderItem(itemId);
                return "ok";
            }
        });
    }
}
