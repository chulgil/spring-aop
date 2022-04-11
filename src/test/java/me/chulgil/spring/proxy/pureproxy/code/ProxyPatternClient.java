package me.chulgil.spring.proxy.pureproxy.code;

public class ProxyPatternClient {

    private ISubject subject;

    public ProxyPatternClient(ISubject subject) {
        this.subject = subject;
    }

    public  void execute() {
        subject.operation();
    }
}
