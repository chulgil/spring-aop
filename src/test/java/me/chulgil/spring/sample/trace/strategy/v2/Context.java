package me.chulgil.spring.sample.trace.strategy.v2;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.sample.trace.strategy.Strategy;


@Slf4j
public class Context {

    private final Strategy strategy;

    public Context(Strategy strategy) {
        this.strategy = strategy;
    }


    public void execute() {
        long startTime = System.currentTimeMillis();
        //비즈니스 로직 실행
        strategy.call(); //위임
        //비즈니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime={}", resultTime);
    }

}
