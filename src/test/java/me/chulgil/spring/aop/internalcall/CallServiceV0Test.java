package me.chulgil.spring.aop.internalcall;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.internalcall.aop.CallLogAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(CallLogAspect.class)
@Slf4j
@SpringBootTest
class CallServiceV0Test {

    @Autowired CallServiceV0 callService;

    @Test
    void external() {
        log.info("target={}", callService.getClass());
        callService.external();
    }

    @Test
    void internal() {
        callService.internal();
    }
}