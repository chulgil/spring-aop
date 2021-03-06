package me.chulgil.spring.aop.pointcut;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ExecutionTest {

    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    Method helloMethod;

    @BeforeEach
    public void init() throws NoSuchMethodException {
        helloMethod = MemberService.class.getMethod("hello", String.class);
    }

    @Test
    void printMethod() {
        //public java.lang.String
        // me.chulgil.spring.aop.member.MemberService.hello(java.lang.String)
        log.info("helloMethod={}", helloMethod);
    }

    @Test
    void exactMatch() {
        pointcut.setExpression("execution(public String me.chulgil.spring.aop.member.MemberService.hello(String))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void allMatch() {
        pointcut.setExpression("execution(* *(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void nameMatch() {
        pointcut.setExpression("execution(* hello(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void nameMatchStar1() {
        pointcut.setExpression("execution(* hel*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void nameMatchStar2() {
        pointcut.setExpression("execution(* *el*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void nameMatchFalse() {
        pointcut.setExpression("execution(* nono(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isFalse();
    }


    @Test
    void packageExactMatch1() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.MemberService.hello(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void packageExactMatch2() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.*.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void packageExactMatchFalse() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.*.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isFalse();
    }

    @Test
    void packageMatchSubPackage1() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member..*.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void packageMatchSubPackage2() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop..*.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }


    @Test
    void typeExactMatch() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.MemberService.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void typeMatchSuperType() {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.IMemberService.*(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    @Test
    void typeMatchInternal() throws NoSuchMethodException {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.MemberService.*(..))");
        Method internalMethod = MemberService.class.getMethod("internal", String.class);
        assertThat(pointcut.matches(internalMethod, MemberService.class)).isTrue();
    }

    //?????????????????? ????????? MemberService ??? internal ????????? ????????? ???????????? ??????. @Test
    @Test
    void typeMatchNoSuperTypeMethodFalse() throws NoSuchMethodException {
        pointcut.setExpression("execution(* me.chulgil.spring.aop.member.IMemberService.*(..))");
        Method internalMethod = MemberService.class.getMethod("internal", String.class);
        assertThat(pointcut.matches(internalMethod, MemberService.class)).isFalse();
    }

    //String ????????? ???????????? ?????? //(String)
    @Test
    void argsMatch() {
        pointcut.setExpression("execution(* *(String))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    //??????????????? ????????? ???
    @Test
    void argsMatchNoArgs() {
        pointcut.setExpression("execution(* *())");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isFalse();
    }

    //????????? ????????? ???????????? ??????, ?????? ?????? ?????? //(Xxx)
    @Test
    void argsMatchStar() {
        pointcut.setExpression("execution(* *(*))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    //????????? ???????????? ?????? ????????????, ?????? ?????? ?????? //??????????????? ????????? ???
    //(), (Xxx), (Xxx, Xxx)
    @Test
    void argsMatchAll() {
        pointcut.setExpression("execution(* *(..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

    //String ???????????? ??????, ????????? ???????????? ?????? ????????????, ?????? ?????? ?????? //(String), (String, Xxx), (String, Xxx, Xxx) ??????
    @Test
    void argsMatchComplex() {
        pointcut.setExpression("execution(* *(String, ..))");
        assertThat(pointcut.matches(helloMethod, MemberService.class)).isTrue();
    }

}