package me.chulgil.spring.proxy.dynamic.code;

import java.lang.reflect.Method;

public interface InvocationHandler {

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;

}
