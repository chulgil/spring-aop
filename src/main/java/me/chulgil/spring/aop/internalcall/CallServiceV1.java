package me.chulgil.spring.aop.internalcall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CallServiceV1 {

    // AOP의 내부호출 대안용
    private CallServiceV1 callServiceV1;

    //    /**
//     * 자기 자신을 생성하면 순환참조가 발생하기 때문에 에러가 발생한다.
//     * @param callServiceV1
//     */
//    public CallServiceV1(CallServiceV1 callServiceV1) {
//        this.callServiceV1 = callServiceV1;
//    }

    @Autowired
    public void setCallServiceV1(CallServiceV1 callServiceV1) {
        this.callServiceV1 = callServiceV1;
    }

    public void external() {
        log.info("call external");
        // 프록시를 적용할 수 없음
        callServiceV1.internal();
    }

    public void internal() {
        log.info("call internal");
    }
}
