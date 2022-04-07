package me.chulgil.spring.sample.app.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class OrderRepository {

    public  void save(String itemId) {

        // 예외 발생 상황 확인용
        if (itemId.equals("ex")) {
            throw new IllegalStateException("예외 발생!");
        }

        // 상품 저장 시간은 1초 정도 걸리는 것으로 가정
        sleep(1000);
    }

    private void sleep(int millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
