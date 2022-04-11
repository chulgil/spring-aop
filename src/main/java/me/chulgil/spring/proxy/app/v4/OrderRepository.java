package me.chulgil.spring.proxy.app.v4;

import me.chulgil.spring.proxy.app.v1.IOrderRepository;

public class OrderRepository implements IOrderRepository {

    @Override
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
