package me.chulgil.spring.sample.trace.strategy.v2;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.sample.trace.strategy.Strategy;
import me.chulgil.spring.sample.trace.strategy.StrategyLogic1;
import me.chulgil.spring.sample.trace.strategy.StrategyLogic2;
import org.junit.jupiter.api.Test;

@Slf4j
public class ContextTest {

    @Test
    void strategyV0() {
        logic1();
        logic2();
    }

    /**
     * 전략 패턴 적용
     */
    @Test
    void strategyV1() {
        Strategy strategyLogic1 = new StrategyLogic1();
        Context context1 = new Context(strategyLogic1);
        context1.execute();
        Strategy strategyLogic2 = new StrategyLogic2();
        Context context2 = new Context(strategyLogic2);
        context2.execute();
    }


    /**
     * 전략 패턴 익명 내부 클래스
     */
    @Test
    void strategyV2() {

        Strategy strategyLogic1 = new Strategy() {
            @Override
            public void call() {
                log.info("비즈니스 로직1 실행");
            }
        };
        log.info("strategyLogic1={}", strategyLogic1.getClass());
        Context context1 = new Context(strategyLogic1);
        context1.execute();

        Strategy strategyLogic2 = new Strategy() {
            @Override
            public void call() {
                log.info("비즈니스 로직2 실행");
            }
        };
        log.info("strategyLogic2={}", strategyLogic2.getClass());
        Context context2 = new Context(strategyLogic2);
        context2.execute();
    }

    /**
     * 전략 패턴 익명 내부 클래스2
     */
    @Test
    void strategyV3() {
        Context context1 = new Context(new Strategy() {
            @Override
            public void call() {
                log.info("비즈니스 로직1 실행");
            }
        });
        context1.execute();

        Context context2 = new Context(new Strategy() {
            @Override
            public void call() {
                log.info("비즈니스 로직2 실행");
            }
        });
        context2.execute();
    }

    /**
     * 전략 패턴, 람다
     */
    @Test
    void strategyV4() {
        Context context1 = new Context(() -> log.info("비즈니스 로직1 실행"));
        context1.execute();

        Context context2 = new Context(() -> log.info("비즈니스 로직2 실행"));
        context2.execute();
    }

    private void logic1() {
        long startTime = System.currentTimeMillis();
        //비즈니스 로직 실행
        log.info("비즈니스 로직1 실행");
        //비즈니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime={}", resultTime);
    }

    private void logic2() {
        long startTime = System.currentTimeMillis();
        //비즈니스 로직 실행
        log.info("비즈니스 로직2 실행");
        //비즈니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime={}", resultTime);
    }
}