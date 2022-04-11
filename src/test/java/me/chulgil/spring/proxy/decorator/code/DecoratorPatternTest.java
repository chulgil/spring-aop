package me.chulgil.spring.proxy.decorator.code;

import org.junit.jupiter.api.Test;

public class DecoratorPatternTest {

    @Test
    void noDecorators() {
        IComponent component = new RealComponent();
        DecoratorPatternClient client = new DecoratorPatternClient(component);
        client.execute();
    }

    @Test
    void decorator() {
        IComponent component = new RealComponent();
        IComponent messageDecorator = new MessageDecorator(component);
        DecoratorPatternClient client = new DecoratorPatternClient(messageDecorator);
        client.execute();
    }

    @Test
    void decoratorTime() {
        IComponent component = new RealComponent();
        IComponent messageDecorator = new MessageDecorator(component);
        IComponent timeDecorator = new TimeDecorator(messageDecorator);
        DecoratorPatternClient client = new DecoratorPatternClient(timeDecorator);
        client.execute();
    }
}
