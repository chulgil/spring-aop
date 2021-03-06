package me.chulgil.spring.sample.app.v3;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.sample.trace.TraceStatus;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final LogTrace trace;

    public  void save(String itemId) {

        TraceStatus status = null;
        try {
            status = trace.begin("OrderRepository.save()");

            // 예외 발생 상황 확인용
            if (itemId.equals("ex")) {
                throw new IllegalStateException("예외 발생!");
            }

            // 상품 저장 시간은 1초 정도 걸리는 것으로 가정
            sleep(1000);

            trace.end(status);
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }

    private void sleep(int millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
