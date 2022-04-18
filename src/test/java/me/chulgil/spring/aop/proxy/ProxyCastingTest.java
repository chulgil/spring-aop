package me.chulgil.spring.aop.proxy;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.member.IMemberService;
import me.chulgil.spring.aop.member.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Member;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class ProxyCastingTest {

    @Test
    void jdkProxy() {
        MemberService target = new MemberService();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(false); // JDK 동적 프록시

        // 프록시를 인터페이스로 캐스팅 성공
        IMemberService memberServiceProxy = (IMemberService) proxyFactory.getProxy();
        log.info("proxy class={}", memberServiceProxy.getClass());

        // JDK동적 프록시를 구현 클래스로 캐스팅 시도 실패, ClassCastException 예외 발생
        assertThrows(ClassCastException.class, () -> {
            MemberService castingMemberService = (MemberService) memberServiceProxy;
        });

    }

    @Test
    void cglibProxy() {
        MemberService target = new MemberService();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true); // CGLIB 프록시

        // 프록시를 인터페이스로 캐스팅 성공
        IMemberService memberServiceProxy = (IMemberService) proxyFactory.getProxy();
        log.info("proxy class={}", memberServiceProxy.getClass());

        // CGLIB 프록시를 구현 클래스로 캐스팅 시도 성공
        MemberService castingMemberService = (MemberService) memberServiceProxy;
    }

}
