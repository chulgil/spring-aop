package me.chulgil.spring.aop.internalcall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CallServiceV0 {

    public void external() {
        log.info("call external");

        // 프록시를 적용할 수 없음
        this.internal();
    }

    public void internal() {
        log.info("call internal");
    }
}
