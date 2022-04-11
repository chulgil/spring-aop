package me.chulgil.spring.proxy.pureproxy.code;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CacheProxy implements ISubject {

    private ISubject target;
    private String cacheValue;

    public CacheProxy(ISubject target) {
        this.target = target;
    }


    @Override
    public String operation() {
        log.info("프록시 호출");
        if (cacheValue == null) {
            cacheValue = target.operation();
        }
        return cacheValue;
    }
}
