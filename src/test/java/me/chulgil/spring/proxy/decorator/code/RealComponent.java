package me.chulgil.spring.proxy.decorator.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealComponent implements IComponent {
    @Override
    public String operation() {
        log.info("RealComponent 실행");
        return "data";
    }
}
