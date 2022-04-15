package me.chulgil.spring.aop.pointcut;

import me.chulgil.spring.aop.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class WithinTest {

    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    Method helloMethod;

    @BeforeEach
    public void init() throws NoSuchMethodException {
        helloMethod = MemberService.class.getMethod("hello", String.class);
    }

    @Test
    void withinExact() {
        pointcut.setExpression("within(me.chulgil.spring.aop.member.MemberService)");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void withinStar() {
        pointcut.setExpression("within(me.chulgil.spring.aop.member.*Service*)");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void withinSubPackage() {
        pointcut.setExpression("within(me.chulgil.spring.aop..*)");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    @DisplayName("타켓의 타입에만 직접 적용, 인터페이스를 선정하면 안된다.")
    void withinSuperTypeFalse() {
        pointcut.setExpression("within(me.chulgil.spring.aop.member.IMemberService)");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isFalse();
    }

    @Test
    @DisplayName("execution은 타입 기반, 인터페이스 선정 가능")
    void executionSuperTypeTrue() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.MemberService.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }
}
