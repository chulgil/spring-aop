# spring-aop

## 1. 로그추적기

### 요구사항 분석

> 애플리케이션이 커지면서 어떤 부분에서 병목과 예외가 발생하는지 로그를 통해 확인하고자 한다.
> 
> 기존에는 문제 발생시 관련부분을 어렵게 찾아서 로그를 남겼지만 이부분을 개선하고 자동화하는 것이 요구사항이다.

### 요구사항

1. 모든 Public 메서드의 호출과 응답 정보를 로그로 출력
2. 애플리케이션의 흐름을 변경하면 안됨
   - 로그를 남긴다고 해서 비지니스 로직의 동작에 영향을 주면 안됨
3. 메서드 호출에 걸린시간 표기
4. 정상 / 예외 흐름 구분하기 : 예외 발생시 정보 표기
5. 메서드 호출의 깊이 표현
6. HTTP 요청을 구분
  - HTTP 요청 단위로 특정ID를 남겨서 어떤 HTTP요청에서 시작된 것인지 명확하게 구분
  - 트랜잭션 ID란 하나의 HTTP요청의 시작과 끝을 하나의 트랜젹션이라함

### 예시
```console
정상 요청
    [796bccd9] OrderController.request()
    [796bccd9] |-->OrderService.orderItem()
    [796bccd9] |   |-->OrderRepository.save()
    [796bccd9] |   |<--OrderRepository.save() time=1004ms
    [796bccd9] |<--OrderService.orderItem() time=1014ms
    [796bccd9] OrderController.request() time=1016ms
예외 발생
[b7119f27] OrderController.request()
[b7119f27] |-->OrderService.orderItem()
[b7119f27] | |-->OrderRepository.save() 
[b7119f27] | |<X-OrderRepository.save() time=0ms ex=java.lang.IllegalStateException: 예외 발생! 
[b7119f27] |<X-OrderService.orderItem() time=10ms ex=java.lang.IllegalStateException: 예외 발생! 
[b7119f27] OrderController.request() time=11ms ex=java.lang.IllegalStateException: 예외 발생!
```


### 로그추적기 V1 테스트

> 요구사항 1-4 까지 구현 

```console
curl http://localhost:8080/v1/request\?itemId\=hello
```
>실행 로그
```console
HelloTraceV1    : [ed67e946] OrderController.request()
HelloTraceV1    : [97bdb531] OrderService.orderItem()
HelloTraceV1    : [21f39438] OrderRepository.save()
HelloTraceV1    : [21f39438] OrderRepository.save() time=1000ms
HelloTraceV1    : [97bdb531] OrderService.orderItem() time=1001ms
HelloTraceV1    : [ed67e946] OrderController.request() time=1002ms
```
---
### 로그추적기 V2 : 파라미터로 동기화 개발
> 메서드 호출 깊이를 표현하고 HTTP 요청단위로 구분
> 
> 처음 로그에서 사용한 상태정보(트랜잭션ID와 level)를 다음 로그로 넘겨주면 된다.

- 즉 TraceId에 상태정보가 포함되어 있기때문에 다음로그로 넘겨준다.
- 기존 TranceId에서 createNextId()를 통해 다음 ID를 구한다.
  - 트랜잭션ID는 기존과 같이 유지하고 깊이를 표현하는 level은 1 증가
  

> 정상 실행 로그
```console
curl http://localhost:8080/v2/request\?itemId\=hello
```

```console
HelloTraceV2    : [ceadf128] OrderController.request()
HelloTraceV2    : [c24461a4] |-->OrderService.orderItem()
HelloTraceV2    : [c24461a4] |   |-->OrderRepository.save()
HelloTraceV2    : [c24461a4] |   |<--OrderRepository.save() time=1006ms
HelloTraceV2    : [c24461a4] |<--OrderService.orderItem() time=1011ms
HelloTraceV2    : [ceadf128] OrderController.request() time=1012ms
```

> 예외 실행 로그
```console
curl http://localhost:8080/v2/request\?itemId\=ex
```

```console
HelloTraceV2    : [830d6e00] OrderController.request()
HelloTraceV2    : [78d3cdac] |-->OrderService.orderItem()
HelloTraceV2    : [78d3cdac] |   |-->OrderRepository.save()
HelloTraceV2    : [78d3cdac] |   |<X-OrderRepository.save() time=0ms ex=java.lang.IllegalStateException: 예외 발생!
HelloTraceV2    : [78d3cdac] |<X-OrderService.orderItem() time=0ms ex=java.lang.IllegalStateException: 예외 발생!
HelloTraceV2    : [830d6e00] OrderController.request() time=1ms ex=java.lang.IllegalStateException: 예외 발생!
```

이로서 모든 요구사항을 구현했지만 아래와 같은 단점이 존재한다.
1. HTTP 요청 구분을 위해 `TranceId` 동기화가 필요하다.
2. 동기화를 위해 관련 메서드의 모든 파라미터를 수정해야 한다.
3. 로그 처음 시작시 `begin()`을 호출하고, 두번째 부터는 `beginSync()`를 호출해야 한다. 
4. 컨트롤러 -> 서비스 호출이 아닌 서브모듈 -> 서비스 호출상황에서는 파라미터로 넘길 `TranceId`가 존재하지 않는다.

---
### 로그추적기 V3 : 필드 동기화 개발

> V2의 단점인 `TranceId`을 파라미터로 넘기지 않기위해 인터페이스를 이용하여 새로운 로그추적기 V3을 구현한다.
> 
> 인터페이스를 구현하고 `TranceIdHolder`필드를 사용해서 파라미터 추가 없는 깔끔한 로그 추적기를 완성했지만
> 
> 실제 서비스 배포시에는 아래와 같은 동시성 호출 문제가 발생한다.
```console
curl http://localhost:8080/v2/request\?itemId\=hello &
curl http://localhost:8080/v2/request\?itemId\=hello
```

```console
FieldLogTrace     : [3017dee5] OrderController.request()
FieldLogTrace     : [3017dee5] |-->OrderService.orderItem()
FieldLogTrace     : [3017dee5] |   |-->OrderRepository.save()
FieldLogTrace     : [3017dee5] |   |   |-->OrderController.request()
FieldLogTrace     : [3017dee5] |   |   |   |-->OrderService.orderItem()
FieldLogTrace     : [3017dee5] |   |   |   |   |-->OrderRepository.save()
FieldLogTrace     : [3017dee5] |   |<--OrderRepository.save() time=1008ms
FieldLogTrace     : [3017dee5] |<--OrderService.orderItem() time=1015ms
FieldLogTrace     : [3017dee5] OrderController.request() time=1017ms
FieldLogTrace     : [3017dee5] |   |   |   |   |<--OrderRepository.save() time=1002ms
FieldLogTrace     : [3017dee5] |   |   |   |<--OrderService.orderItem() time=1003ms
FieldLogTrace     : [3017dee5] |   |   |<--OrderController.request() time=1003ms
```

`FieldLogTrace` 는 싱글톤으로 등록된 스프링 빈이다. 이 객체의 인스턴스가 애플리케이션에 하나 존재하기 때문에
여러 쓰레드에서 `FieldLogTrace.traceIdHolder` 동시에 접근하면 이런 문제가 발생한다.

---
## 2. 동시성 문제 

### 동시성 문제 테스트 코드 작성

> FieldServiceTest -> feild() 를 실행하게 되면 결과는 아래와 같다. 

```console
FieldServiceTest - main start
FieldService - 저장 name=userA -> name
FieldService - 저장 name=userB -> name
FieldService - 조회 nameStore=userB
FieldService - 조회 nameStore=userB
FieldServiceTest - main exit
```

> 기대 값은 userA와 userB가 조회 되어야 하지만 userB만 조회 되므로 동시성 이슈가 발생 함을 알 수 있다.
>  
> 지역 변수는 쓰레드 마다 각각 다른 메모리 영역이 할당 되므로 동시성 이슈는 없다.
> 
> 싱글톤 또는 static 필드에 접근할 때 어디선가 값을 변경하게 되면 동시성 이슈가 발생하게 된다.

---
### 로그추적기 V3 : 쓰레드 동기화 개발

```console
curl http://localhost:8080/v3/request\?itemId\=hello &
curl http://localhost:8080/v3/request\?itemId\=hello
```

```console
ThreadLocalLogTrace   : [e2a2cd5b] OrderController.request()
ThreadLocalLogTrace   : [e2a2cd5b] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [e2a2cd5b] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [e2a2cd5b] |   |<--OrderRepository.save() time=1004ms
ThreadLocalLogTrace   : [e2a2cd5b] |<--OrderService.orderItem() time=1004ms
ThreadLocalLogTrace   : [e2a2cd5b] OrderController.request() time=1005ms

ThreadLocalLogTrace   : [ceb76639] OrderController.request()
ThreadLocalLogTrace   : [ceb76639] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [ceb76639] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [ceb76639] |   |<--OrderRepository.save() time=1001ms
ThreadLocalLogTrace   : [ceb76639] |<--OrderService.orderItem() time=1003ms
ThreadLocalLogTrace   : [ceb76639] OrderController.request() time=1004ms
```
> 동시에 실행 하여도 로그가 의도 대로 나누어 진 것을 확인할 수 있다.
> 쓰레드 로컬의 값을 사용후 제거하지 않으면 WAS(톰캣)처럼 쓰레드 풀을 사용하는 경우에 심각한 문제가 발생할 수 있다.

1. UserA의 HTTP 할당 요청
2. WAS는 쓰레드 풀에서 하나 조회
3. 쓰레드 threadA 할당
4. threadA는 UserA의 데이터를 쓰레드 로컬에 저장
5. UserA의 HTTP 응답 종료
6. WAS는 사용이 끝난 threadA를 쓰레드 풀에서 반환
7. 쓰레드 생성비용이 비싸므로 쓰레드 재사용 
8. threadA 전용 보관소에 UserA데이터가 존재



1. UserB의 HTTP 할당 요청
2. WAS는 쓰레드 풀에서 하나 조회
3. 쓰레드 threadA 할당
4. threadA는 쓰레드 로컬에서 데어터 조회
5. 쓰레드 로컬은 threadA의 UserA값 반환

> 결과적으로 UserB는 UserA의 데이터를 확인하게 되는 심각한 문제가 발생하게 되므로
> 
> 이런 문제를 예방하려면 UserA의 요청이 끝날대 로컬의 값을 `ThreadLocal.remove()`를 통해서 꼭 제거해야 한다.
>

---
## 3. 템플릿 메서드 패턴과 콜백 패턴

### 템플릿 메서드 패턴 - 시작

> 좋은 설계는 변하는 것과 변하지 않는 것을 분리 하는 것이다.
> 즉 핵심 기능은 변하고, 로그 추적기를 사용하는 부분은 변하지 않는다.
> 템플릿 메서드 패턴은 이런 문제를 해결하는 디자인 패턴이다.

> TemplateMethodTest -> templateMethod()을 호출하면 아래와 같은 실행 결과가 나온다.
> 아래 비지니스 로직과 실행 시간을 분리 해야 한다.

```console
비즈니스 로직1 실행 
resultTime=5
비즈니스 로직2 실행 
resultTime=1
```

### 템플릿 메서드 패턴 - 다형성

> Abstract클래스를 생성하고 하위 클래스에서 call() 메서드를 오버라이딩하여 분리 하는 방법이 있다.
> 하지만 하위 클래스를 계속 만들어야 하는 단점이 있다.

### 템플릿 메서드 패턴 - 익명 내부 클래스

익명 내부 클래스는 객체 인스턴스 생성과 동시에 상속 받은 자식클래스를 정의 할 수 있다.
실행 결과를 보면 자바가 임의로 내부클래스 이름을 생성해 주는 것을 알 수 있다.

TemplateMethodTest -> templateMethod()을 호출하면 아래와 같은 실행 결과가 나온다.

```console
TemplateMethodTest - 클래스 이름1=class me.chulgil.spring.sample.trace.template.TemplateMethodTest$1
TemplateMethodTest - 비즈니스 로직1 실행
AbstractTemplate - resultTime=0
TemplateMethodTest - 클래스 이름2=class me.chulgil.spring.sample.trace.template.TemplateMethodTest$2
TemplateMethodTest - 비즈니스 로직2 실행
AbstractTemplate - resultTime=0
```

---
### 로그추적기 V4 : 템플릿 메서드 패턴
> 적용한 실행 결과는 아래와 같다.

```console
curl http://localhost:8080/v4/request\?itemId\=hello &curl http://localhost:8080/v4/request\?itemId\=hello
```

```console
ThreadLocalLogTrace   : [120c2d30] OrderController.request()
ThreadLocalLogTrace   : [120c2d30] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [120c2d30] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [120c2d30] |   |<--OrderRepository.save() time=1002m
ThreadLocalLogTrace   : [120c2d30] |<--OrderService.orderItem() time=1003ms
ThreadLocalLogTrace   : [120c2d30] OrderController.request() time=1005ms
ThreadLocalLogTrace   : [372c2af8] OrderController.request()
ThreadLocalLogTrace   : [372c2af8] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [372c2af8] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [372c2af8] |   |<--OrderRepository.save() time=1011m
ThreadLocalLogTrace   : [372c2af8] |<--OrderService.orderItem() time=1011ms
ThreadLocalLogTrace   : [372c2af8] OrderController.request() time=1011ms
```

지금까지 로그 추적기를 생성한 것을 보면 아래와 같다.

- 로그추적기 V0 : 핵심 기능만 있다.
- 로그추적기 V3 : 핵심 기능과 부가 기능이 섞여 있다.
- 로그추적기 V4 : 핵심 기능과 템플릿을 호출하는 코드가 섞여 있다.

이로서 V4에서 로그를 남기는 부분은 단일 책임 원칙(SRP)을 적용 하였다.
따라서 변경 지점을 하나로 모아서 변경에 쉽게 대처 할 수 있다.
하지만 상속에 강하게 의존하고 있기때문에 부모 클래스를 수정하면 자식 클래스에도 영향을 주는 단점이 존재한다.
또한 별도의 클래스나 익명 내부 클래스를 만들어야 하는 복잡함도 존재한다.

템플릿 메서드 패턴과 비슷한 역할을 하면서 상속의 단점을 제거할 수 있는 디자인 패턴이 있다.
바로 Strategy Pattern 이다.

---
### 로그추적기 V4 : 전략 패턴

> 전략 패턴은 변하지 않는 부분을 `Context`에 두고, 번하는 부분을 `Strategy`라는 인터페이스를 구현하여 문제를 해결한다.
> 
> 즉 `Context`는 템플릿이고 `Strategy`는 변하는 알고리즘 역할을 한다.

스프링으로 애플리케이션을 개발할 때 로딩 시점에 의존 관계 주입을 통해 실제 요청을 처리하는 것과 같은 원리이다.
이 방식의 단점은 조립이후에는 전략을 변경하기가 번거롭다는 점이다.
Context에 setter를 제공해서 전략을 변경하면 되지만
Context를 싱글톤으로 사용할 때는 동시성 이슈 등 고려할 점이 많다.
따라서 전략을 실시간으로 변경해야 한다면 차라리 Context를 하나더 생성하고 그곳에 다른 전략을 주입하는 것이 더 나은 선택일 수 있다.

이렇게 먼저 조립하고 사용하는 방식보다는 유연하게 전략 패턴을 사용하는 방법이 있다.

진략을 실행할 때 직접 파라미터로 전달해서 사용하는 방법이다.

1. 클라이언트는 Context를 실행하면서 인수로 Strategy를 전달한다.
2. Context는 execute() 로직을 실행한다.
3. Context는 파라미터로 넘어온 strategy.call() 로직을 실행한다.
4. Context의 execute() 로직이 종료된다.

이 방법은 실행할 때 마다 전략을 유연하게 변경할 수 있지만 
실행할 때 마다 전략을 지정해 주어야 하는 단점이 존재 한다.

---
### 로그추적기 V5 :  템플릿 콜백 패턴

> 상기 전략패턴의 두번째 방법은 변하지 않는 템플릿 역할을 한다. 변하는 부분은 파라미터로 넘어온 Strategy의 코드를 실행해서 처리한다.
> 이렇게 다른 코드의 인수로서 넘겨주는 실행 가능한 코드를 콜백(callback)이라 한다.
> 스프링에서는 상기 전략패턴을 `템플릿 콜백 패턴`이라 한다.


```console
curl http://localhost:8080/v5/request\?itemId\=hello &curl http://localhost:8080/v5/request\?itemId\=hello
```

```console
ThreadLocalLogTrace   : [592756ac] OrderController.request()
ThreadLocalLogTrace   : [592756ac] |-->OrderController.request()
ThreadLocalLogTrace   : [592756ac] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [592756ac] |   |<--OrderRepository.save() time=1004m
ThreadLocalLogTrace   : [592756ac] |<--OrderController.request() time=1004ms
ThreadLocalLogTrace   : [592756ac] OrderController.request() time=1005ms

ThreadLocalLogTrace   : [3b3d4313] OrderController.request()
ThreadLocalLogTrace   : [3b3d4313] |-->OrderController.request()
ThreadLocalLogTrace   : [3b3d4313] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [3b3d4313] |   |<--OrderRepository.save() time=1001m
ThreadLocalLogTrace   : [3b3d4313] |<--OrderController.request() time=1002ms
ThreadLocalLogTrace   : [3b3d4313] OrderController.request() time=1004ms
```

> 이로서 변하는 코드와 변하지 않는 코드를 적용하고 콜백으로 람다를 사용하여 코드 사용도 최소화 하였다.
> 그런데 아무리 최적화를 하여도 결국 로그 추적기를 적용하기 위해서 원본 코드를 수정해야 한다는 단점이 존재한다.
> 원본 코드를 손대지 않고 로그 추적기를 적용할 수 있는 방법 또한 존재한다.
---
### 프록시 패턴

프록시를 활용하면 아래 와 같은 기능을 활용 할 수 있다. 

1. 권한에 따른 접근 차단
2. 캐싱
3. 지연 로딩
4. 요청 값이나 응답 값을 중간에 변형 (데코레이터)
5. 실행 시간을 측정 및 로그 출력 (데코레이터)

GOF의 디자인패턴에서의 프록시 패턴은 접근제어가 목적이고 데코레이터 패턴은 새로운 기능 추가가 목적이지만
데코레이터 패턴도 프록시를 사용한다.

Client -> Server 에서 Client -> Proxy 로 DI를 사용해서 객체 의존관계를 변경하면 
로그추적기 클라이언트 코드를 변경하지 않아도 기능 구현이 가능하다.

InterfaceProxyConfig를 통해 프록시를 적용하면 프록시 객체는 스프링 컨테이너가 관리하고 자바 힙 메모리에도 올라간다.

반면에 실제 객체는 자바 힙 메모리에는 올라가지만 스프링 컨테이너가 관리하지는 않는다.

런터임 객체 의존 관계는 다음과 같다.
```console
client -> controllerProxy -> controller -> serviceProxy -> service 
```

> 실행결과 
```console
curl http://localhost:8080/v1/request\?itemId\=hello
```

```console
ThreadLocalLogTrace   : [5305b416] OrderController.request()
ThreadLocalLogTrace   : [5305b416] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [5305b416] |   |-->OrderRepository.request()
ThreadLocalLogTrace   : [5305b416] |   |<--OrderRepository.request() time=1005ms
ThreadLocalLogTrace   : [5305b416] |<--OrderService.orderItem() time=1007ms
ThreadLocalLogTrace   : [5305b416] OrderController.request() time=1007ms
```

> 인터페이스 기반 프록시와 클래스 기반 프록시

- 인터페이스가 없어도 클래스 기반으로 프록시를 생성할 수 있다.
- 클래스는 해당 클래스에만 적용할 수 있고 인터페이스는 같은 인터페이스를 사용하는 모든 곳에 적용할 수 있다.
- 클래스 기반 프록시는 상속을 사용하기 때문에 부모클래스의 생성자를 호출해야하는 점과 final 키워드가 있으면 상속이 불가능한 단점이 있다.
- 인터페이스 기반 프록시의 단점은 인터페이스가 필요하다는 것과  캐스팅관련 단점이 있다.

> 지금까지 작성한 프록시 클래스는 대상 클래스가 100개 있다면 프록시 클래스도 100개를 다 만들어야 하는 단점이 존재한다.
>
> 이 부분을 동적 프록시 기술로 해결할 수 있다.

### JDK 프록시 패턴

동적 프록시 또한 인터페이스가 필수이기 때문에 V1 애플리케이션에 적용한다.

실행 순서는 다음과 같다.
1. 클라이언트는 JDK동적 프록시의 call() 실행
2. 프록시는 (TimeInvocationHandler) InvocationHandler.invoke() 호출
3. 내부로직 수행후 method.invoke()를 호출하여 (AImple) target을 호출
4. AImple 인스턴스의 call() 실행
5. TimeInvocationHandler로 응답이 오면 로그 출력후 겶과 반환

동적 프록시 기술 덕분에 적용대상 만큼 프록시 객체를 만들지 않아도 되고 부가 기능 로직을 한번 개발후 공통으로 사용 가능하다.
결과적으로 하나의 클래스에 모아서 단일 책임 원칙을 지킬 수 있게 하였다.

> 실행결과
```console
curl http://localhost:8080/v1/request\?itemId\=hello
```

```console
ThreadLocalLogTrace   : [eb2dd8a4] IOrderController.request()
ThreadLocalLogTrace   : [eb2dd8a4] |-->IOrderService.orderItem()
ThreadLocalLogTrace   : [eb2dd8a4] |   |-->IOrderRepository.save()
ThreadLocalLogTrace   : [eb2dd8a4] |   |<--IOrderRepository.save() time=1001ms
ThreadLocalLogTrace   : [eb2dd8a4] |<--IOrderService.orderItem() time=1001ms
ThreadLocalLogTrace   : [eb2dd8a4] IOrderController.request() time=1002ms
```

> 로그 출력시 메서드 이름으로 필터 기능 추가

특정 메서드 이름이 매칭되는 경우에만 LogTrace로직을 실행한다.

스프링이 제공하는 PatternMatchUtils.simpleMatch(..) 를 사용하면 매칭 로직을 쉽게 적용 가능하다.
- xx : xx가 정확히 매칭되면 True
- xx* : xx로 시작하면 True
- *xx : xx로 끝나면 True
- *xx* : xx가 있으면 True
- String[] patterns : 적용할 패턴은 생성자를 통해 외부에서 받는다.

### CGLIB : Code Generator Library
> 바이트 코드를 조작해서 동적으로 클래스를 생성해주는 라이브러리
> 인터페이스 없이 클래스만 가지고 동적 프록시 생성 가능
> 스프링 프레임워크의 내부 소스코드에 외부 라이브러리인 CGLIB가 포함됨


CGLIB가 생성한 클래스의 이름은 다음 규칙과 같다.
```console
대상클래스$$EnhancerByCGLIB$$임의코드
ConcreteService$$EnhancerByCGLIB$$9e5e6870
```

> 상속을 사용하므로 기존 기존 클래스 기반 프록시의 단점이 존재한다.
> 
> 따라서 인터페이스가 있는 경우 JDK동적 프록시를 사용하고 그렇지 않은경우 CGLIB를 사용하면 된다.

다음번 해결 과제
- 두 기술을 같이 사용한다면 부가기능 제공으로 인한 JDK->InvocationHandler와 MethodInterceptor를 중복으로 만들어서 관리
- 특정 조건에만 프록시 로직을 적용하는 기능도 공통으로 제공되어야 한다.

---



















