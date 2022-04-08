package me.chulgil.spring.sample.app.v5;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.sample.trace.callback.TraceTemplate;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import me.chulgil.spring.sample.trace.template.AbstractTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class OrderRepository {

    private final TraceTemplate template;

    public OrderRepository(LogTrace trace) {
        this.template = new TraceTemplate(trace);
    }

    public  void save(String itemId) {

        template.execute("OrderRepository.save()", () -> {
            if (itemId.equals("ex")) {
                throw new IllegalStateException("예외 발생!");
            }
            sleep(1000);
            return null;
        });
    }

    private void sleep(int millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
