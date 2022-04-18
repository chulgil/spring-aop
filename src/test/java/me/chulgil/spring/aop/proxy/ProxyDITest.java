package me.chulgil.spring.aop.proxy;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.member.IMemberService;
import me.chulgil.spring.aop.member.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
//@SpringBootTest(properties = {"spring.aop.proxy-target-class=false"}) // JDK동적 프록시 , DI 예외 발생
//@SpringBootTest(properties = {"spring.aop.proxy-target-class=true"}) // CGLIB 프록시 , 성공
@SpringBootTest
@Import(ProxyDIAspect.class)
class ProxyDITest {

    @Autowired
    IMemberService iMemberService; // JDK 동적 프록시 OK, CGLIB OK
    @Autowired
    MemberService memberService;

    @Test
    void go() {
        log.info("iMemberService class={}", iMemberService.getClass());
        log.info("memberService class={}", memberService.getClass());
        memberService.hello("hello");
    }
}