package me.chulgil.spring.proxy.concrete.code;

public class ConcreteClient {

    // ConcreteLogic, TimeProxy 모두 주입가능
    private ConcreteLogic concreteLogic;

    public ConcreteClient(ConcreteLogic concreteLogic) {
        this.concreteLogic = concreteLogic;
    }

    public void execute() {
        concreteLogic.operation();
    }
}
