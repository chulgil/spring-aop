package me.chulgil.spring.proxy.app.v3;

import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    public void save(String itemId) {

        // 예외 발생 상황 확인용
        if (itemId.equals("ex")) {
            throw new IllegalStateException("예외 발생!");
        }
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
