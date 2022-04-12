package me.chulgil.spring.proxy.dynamic.code;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class ReflectionTest {


    @Test
    void reflectionTest() {
        Hello target = new Hello();

        // 공통 로직 시작
        log.info("start");
        String resA = target.callA(); // 호출하는 메서드가 다름
        log.info("result={}", resA);
        // 공통 로직 종료

        // 공통 로직 시작
        log.info("start");
        String resB = target.callB(); // 호출하는 메서드가 다름
        log.info("result={}", resB);
        // 공통 로직 종료


        // 호출하는 대상이 다르므로, 동적처리 필요
        //        log.info("start");
        //        String resC = target.XXX();
        //        log.info("result={}", resB);
    }

    @Test
    void reflectionTest2() throws Exception {

        // 클래스 정보
        Class<?> classHello = Class.forName("me.chulgil.spring.proxy.dynamic.code.ReflectionTest$Hello");

        Hello target = new Hello();
        Method callA = classHello.getMethod("callA");
        Object res = callA.invoke(target);
        log.info("result={}", res);

    }

    @Test void reflectionTest3() throws Exception {
        Class<?> classHello = Class.forName("me.chulgil.spring.proxy.dynamic.code.ReflectionTest$Hello");

        Method callA = classHello.getMethod("callA");
        dynamicCall(callA, new Hello());
        Method callB = classHello.getMethod("callB");
        dynamicCall(callB, new Hello());
    }

    private void dynamicCall(Method method, Object target) throws Exception {
        log.info("start");
        Object result = method.invoke(target);
        log.info("result={}", result);
    }


    @Slf4j
    static class Hello {

        public String callA() {
            log.info("CallingA");
            return "A";
        }

        public String callB() {
            log.info("CallingB");
            return "B";
        }
    }

}

