package me.chulgil.spring.aop.member;

import me.chulgil.spring.aop.member.annotation.ClassAop;
import me.chulgil.spring.aop.member.annotation.MethodAop;
import org.springframework.stereotype.Component;

@ClassAop
@Component
public class MemberService implements IMemberService {

    @Override
    @MethodAop("test value")
    public String hello(String param) {
        return "ok";
    }

    public String internal(String param) {
        return "ok";
    }
}
