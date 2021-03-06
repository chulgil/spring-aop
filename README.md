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
## 4. 프록시 패턴과 데코레이터 패턴

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
---

## 5. JDK 프록시 패턴

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
## 6. 스프링이 제공하는 프록시

### 프록시 팩토리

> 스프링은 유사한 기술이 있을때 통합해서 일관성 있게 접근할 수 있는 추상화된 기술을 제공한다.
> 
> 그중 동적 프록시를 편리하게 만들어주는 ProxyFactory라는 기능을 제공한다.
>
> 프록시 팩토리는 인터페이스가 있으면 JDK동적 프록시를 사용하고, 구체 클래스라면 CGLIB를 사용한다.

사용흐름은 아래와 같다.

1. 클라이언트에서 프록시요청
2. 프록시 기술선택 -> ProxyFactory
3. 프록시 생성 (JDK동적 프록시 / CGLIB)
4. 프록시 반환 (<<ServiceInterface>> or <<ConcreteService>>)

- Advice :
  여기서 두 프록시를 선택할때 스프링은 Advice라는 새로운 개념을 도입하여 개발자는 InvocationHandler나 MethodInterceptor를
신경쓰지 않아도 된다.
- Pointcut :
  또한 특정조건에만 적용하는 필터기능도 Pointcut이라는 개념을 도입하여 일관성있게 해결하였다.

> 스프링 부트는 AOP를 적용할 때 기본적으로 proxyTargetClass=true로 설정해서 사용한다.
> 
> 따라서 인터페이스가 있어도 항상 CGLIB를 사용하여 구체클래스 기반으로 프록시를 생성하고 있다.

### 포인트컷 / 어드바이스 / 어드바이저

- 포인트컷 : 어디에 부가 기능을 적용할지 판단하는 필터링 로직으로 주로 클래스와 메서드 이름으로 필터링한다. 

- 어드바이스 : 프록시가 호출하는 부가기능으로 단순 프록시 로직

- 어드바이저 : 단순하게 하나의 포인트 컷과 어드바이스를 담고있음

> 쉽게 기억하자면 
> 조언(Advice)을 어디(Pointcut)에 할 것인가?
> 조언자(Advisor)는 어디(Pointcut)에 조언(Advice) 을 해야 할지 알고 있다.


> 역할과 책임
- 포인트 컷은 대상 여부를 확인하는 필터 역할만 담당
- 어드바이스는 부가 기능 로직만 담당
- 어드바이저는 단순히 위 두개로 구성되어있음


### 프록시 팩토리 적용

스프링은 AOP를 적용할 때 최적화를 진행해서 하나의 프록시에 여러 어드바이저를 적용한다.
즉 하나의 target에 여러 AOP가 동시에 적용 되어도, 스프링의 AOP는 target마다 하나의 프록시만 생성한다.

V1애플리케이션은 인터페이스가 있기 때문에 프록시 팩토리가 JDK동적 프록시를 적용한다.
V2애플리케이션은 구체 클래스만 있기 때문에 CGLIB를 적용한다.

> 실행 로그

```console
http://localhost:8080/v2/request?itemId=hello
```

```console
ThreadLocalLogTrace   : [39857d1f] OrderController.request()
ThreadLocalLogTrace   : [39857d1f] |-->OrderService.orderItem()
ThreadLocalLogTrace   : [39857d1f] |   |-->OrderRepository.save()
ThreadLocalLogTrace   : [39857d1f] |   |<--OrderRepository.save() time=1009ms
ThreadLocalLogTrace   : [39857d1f] |<--OrderService.orderItem() time=1014ms
ThreadLocalLogTrace   : [39857d1f] OrderController.request() time=1019ms
```

프록시 팩토리 덕분에 개발자는 매우 편리하게 프록시를 생성 할 수 있게 되었지만
너무 많은 Config파일을 생성해야한다는 점과
V3처럼 컴포넌트 스캔을 사용하는 경우라면 지금과 같은 방법으로는 프록시 적용이 불가능하다.
실제 객체를 컴포넌트 스캔으로 컨터이너에 빈으로 등록을 다 해버린 상태이기 때문이다.

따라서 ProxyFactoryConfigV1와 같이 부가 기능이 있는 프록시를 실제 객체 대신 스프링 컨테이너에 빈으로 등록해야 한다.

이 두가지 문제를 해결해 주는 방법이 `Bean 후처리기` 이다.


## 7. Bean 후 처리기

### Bean 후처리기 구현

이제 프록시를 생성하는 코드가 설정 파일에는 필요 없다. 순수한 빈 등록만 고민하면 된다.
프록시를 생성하고 프록시를 스프링 빈으로 등록하는 것은 빈 후처리기가 모두 처리해준다.

BeanPostProcessorConfig파일에서 V3는 컴포넌트 스캔으로 자동으로 스프링 빈으로 등록되지만, V1, V2 애플리케이션은 수동으로 스프링 빈으로 등록해야 동작한다.
ProxyApplication 에서 등록해도 되지만 편의상 이 파일에  등록한다.
ex) @Import({AppV1Config.class, AppV2Config.class})

@Bean logTraceProxyPostProcessor() : 특정 패키지를 기준으로 프록시를 생성하는 빈 후처리기를 스프링 빈으로 등록한다. 
빈 후처리기는 스프링 빈으로만 등록하면 자동으로 동작한다. 
여기에 프록시를 적용할 패키지 정보( hello.proxy.app )와 어드바이저( getAdvisor(logTrace) )를 넘겨준다.

- v1: 인터페이스가 있으므로 JDK 동적 프록시가 적용된다.
- v2: 구체 클래스만 있으므로 CGLIB 프록시가 적용된다.
- v3: 구체 클래스만 있으므로 CGLIB 프록시가 적용된다.

빈 후처리기 덕분에 수 많은 스프링 빈이 추가 되어도 프록시와 관련된 코드는 전혀 변경하지 않아도 된다.

### 스프링이 제공하는 Bean 후처리기 구현 

build.gradle 에 다음을 추가한다.

```console
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

이 라이브러리를 추가함으로 aspectjweaver라는 aspectJ 관련 라이브러리를 등록하고 부트가 AOP관련 클래스를 자동으로 빈에 등록해준다.

- @EnableAspectJAutoProxy : 부트가 없던 시절에 이 애노테이션을 직접 사용해야 했음
- AopAutoConfiguration : 스프링 부트가 자동으로 활성화 하는 빈 설정
- AnnotationAwareAspectJAutoProxyCreator : @AspectJ와 관련된 AOP 기능도 자동으로 찾아서 처리

이 빈 후처리기는 빈으로 등록된 Advisor들을 자동으로 찾아서 프록시가 필요한 곳에 자동으로 프록시를 적용해준다.

Advisor안에는 Pointcut과 Advice가 포함되어 있고 Advisor만 알고 있으면 Pointcut으로 어떤 빈에 프록시를 적용해야 할지 알 수 있다.


> 자동 프록시 생성기의 작동과정

1. 생성 : 스프링이 빈 대상이 되는 객체 생성 (@Bean, 컴포넌트 스캔 모두 포함)
2. 전달 : 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달
3. 빈 조회 : 자동 프록시 생성기인 빈 후처리기는 컨테이너에서 모든 Advisor를 조회
4. 적용 대상 체크 : Advisor에 포함되어있는 Pointcut을 사용해서 프록시를 적용할 대상인지 아닌지 판단
5. 프록시 생성 : 적용대상이면 생성후 프록시 반환, 아니라면 원본객체를 반환
6. 빈 등록 : 반환된 객체는 스프링 빈으로 등록된다.

프록시는 내부에 어드바이저와 실제 호출해야할 대상 객체 target을 알고 있다.

AutoProxyConfig를 적용한후 아래 URL로 접속하면 로그가 출력되지 않는 것을 확인 할 수 있다.

```console
http://localhost:8080/v1/no-log
```

어플리케이션을 실행해보면 스프링이 초기화되면서 기대하지 않은 로그들이 올라온다. 왜냐하면 포인트컷에 
`"request*", "order*", "save*"` 가 포함되어 모든파일이 매칭되고 있기 때문이다.

결론적으로 패키지에 메서드 이름까지 함께 지정할 수 있는 정밀한 포인트 컷이 필요하다.

- V1 : NameMatchMethodPointcut : 모든 메서드의 이름이 매칭 대상
- V2 : AspectJExpressionPointcut : 포인트 컷 표현식 적용 
  - execution(* hello.proxy.app..) : 해당 패키지와 그 하위패키지 내 
  - *(..) : 모든 메서드의 이름으로 매칭
  - 단순히 package 기준으로 포인트 컷 매칭을 하기 때문에 lo-log 메서드의 실행 로그가 출력됨
- V3 : AspectJExpressionPointcut : 포인트 컷 표현식 적용 
  - 표현식 추가 : && !execution(*me.chulgil.spring.proxy.app..noLog(..))
  - 패키지와 하위 패키지의 모든 메서드는 포인터컷 매칭하되, noLog 메서드는 제외하라는 설정이다.
  - 다시 실행해보면 noLog로그가 남지 않는다.
  
> 프록시 자동 생성기 상황별 정리

 스프링 빈이 제공하는 포인트 컷을 모두 만족한다면 프록시 자동 생성기는 프록시를 몇개 생성할까? 
 - advisor1의 포인트 컷만 만족 -> 프록시 1개 생성, 프록시에 advisor1만 포함
 - advisor1,2의 포인트 컷을 만족 -> 프록시 1개 생성, 프록시에 advisor1,2 모두 포함
 - advisor1,2의 포인트 컷을 모두 만족X -> 프록시가 생성되지 않음 

스프링의 AOP도 동일한 방식으로 동작한다.

프록시 자동 생성기인 AnnotationAwareAspectJAutoProxyCreator 덕분에 매우 편리하게 프록시를 적용할 수 있다.
이제 Advisor만 스프링 빈으로 등록하면된다.

> 더 편리한 방법으로는 
> @Aspect 애노테이션을 사용해서 더 편리하게 포인트 컷과 어드바이스를 만들고 프록시를 적용하는 방법도 있다.

## 8. Aspect AOP

스프링 애플리케이션에 프록시를 적용하려면 포인트 컷과 어드바이스로 구성되어 있는 어드바이저를 만들어서 빈으로 등록하면 된다.
자동 프록시 생성기가 빈으로 등록된 어드바이저를 찾아서 스프링 빈들에게 자동으로 프록시를 적용해 준다.

- @Aspect : 관전지향 프로그래밍을 가능하게 하는 AspectJ프로젝트에서 제공하는 어드바이저 생성기능
- @Around() : AsprctJ 포인트컷 표현식 사용 @Around를 적용한 메서드는 어드바이스가 된다.
- ProceedingJoinPoint : 어드바이스에서 살펴본 MethodInterceptor 와 유사한 기능이다.
- jointPoint.proceed() : 실제 호출 대상(target) 을 호출한다.
- @Bean logTraceAspect(): @Aspect가 있더라도 스프링 빈으로 등록해 줘야 동작한다.

실행해보면 프록시가 잘 적용 된 것을 확인 할 수 있다.
```console
http://localhost:8080/v1/request?itemId=hello 
http://localhost:8080/v2/request?itemId=hello 
http://localhost:8080/v3/request?itemId=hello
```

@Aspect를 어드바이저로 변환해서 저장하는 과정

1. 실행 : 어플리케이션 로딩 시점에 자동 프록시 생성기 호출
2. 모든 빈 조회 : 생성기는 모든 스프링 컨테이너에서 모든 @Aspect 빈을 조회환다.
3. 어드바이저 생성 : 어드바이저 빌더를 통해 @Aspect정보를 기반으로 어드바이저를 생산한다.
4. 어드바이저 저장 : 생성한 어드바이저를 @Aspect 어드바이저 빌더 내부에 저장한다.

실무에서 프록시 적용시 대부분 이 방법을 사용하는데 이 방법이 바로 여러 곳에 걸쳐 있는 횡단 관심사의 문제를 해결해준다.


## 9. 스프링 AOP 개념

> AspectJ 프레임워크

AOP의 대표적인 구현으로 [AspectJ프레임워크](https://www.eclipse.org/aspectj/)가 있는데 스프링도 AOP를 지원하지만
대부분 AspectJ문법을 차용하고, AspectJ가 제공하는 일부 기능만 제공한다.

### AOP 적용 방식

AOP를 사용하면 핵심 기능과 부가 가능이 코드상 완전히 분리되어서 관리된다.

> 부가 기능 로직을 실제 로직에 추가하는 방법

- 컴파일 시점 : 위빙(Weaving) 원본 로직에 부가기능 로직이 추가되는 형태이다.
- 클래스 로딩 시점 : 
  - .class파일을 JVM내부의 클래스로더에 보관하는데 중간에서 이 클래스파일을 조작한후 JVM에 올릴 수 있다.
  - 많은 모니터링 툴이 이 방식을 사용한다.
  - 로드 타임 위빙은 자바를 실행할 때 특별한 옵션 (java -javaagent)을 통해 클래스 로더 조작기를 지정해야하는데 이부분 번거롭다.
- 런타임 시점(프록시)
  - 컴퍼일이 끝나고 클래스 로더에 클래스도 다 올라가서 이미 자바가 실행되고 난 다음에 실행된다.
  - 따라서 자바 언어가 제공하는 범위 안에서 부가 기능을 적용해야 한다.
  - 즉 스프링과 같은 컨터이너의 도움을 받고 프록시와 DI, 빈 포스트 프로세서 같은 개념을 총 동원해야 한다.
  - 프록시를 사용하기 때문에 AOP기능에 일부 제약이 있다. 
  - 자바 실행시에는 복잡한 옵션과 클래스 로더 조작기를 설정하지 않아도 된다.

> 부가 기능이 적용되는 차이

- 컴파일 시점 : AspectJ직접 사용 - 대상 코드에 애스팩트를 통한 부가기능 호출코드가 포함됨
- 클래스 로딩시점 : AspectJ직접 사용 - 동일
- 런타임 시점 : 실제 대상코드는 그대로 유지되는 반면 프록시를 통해 부가 기능이 적용된다. 항상 프록시를 통해야 부가기능이 사용 가능하다.

> AOP 적용 위치

- 적용가능한 지점(조인 포인트) : 생성자, 필드 값 접근, static 메서드 접근, 메서드 실행
- AspectJ를 사용해서 컴파일 시점과 로딩 시점에 적용하는 AOP는 바이트 코드를 실제 조작하기 때문에 해당 기능을 모든 지점에 다 적용할 수 있다.
- 프록시 방식을 사용하는 스프링 AOP는 메서드 실행 지점 에만 AOP를 적용할 수 있다.
  - 프록시는 메서드 오버라이딩 개념으로 동작하기 때문에 생성자나 static 메서드, 필드 값 접근에는 적용할 수 없다.
  - 스프링 AOP의 조인 포인트는 메서드 실행으로 제한된다.
  - 스프링 컨테이너가 관리할 수 있는 스프링 빈에만 AOP를 적용할 수 있다. 

> AOP 용어 정리


- 조인 포인트 (Join Point)
  - 어드바이스가 적용될 수 있는 지점 (메서드 실행, 생성자 호출, 필드 값 접근, static메서드 접근)
- 포인트 컷 (Pointcut)
  - 조인 포인트 중에서 어드바이스가 적용될 위치를 선별하는 기능
  - 주로 AspectJ 표현식을 사용해서 지정
  - 프록시를 사용하는 스프링AOP는 메서드 싫행 지점만 포인터 컷으로 선별 가능
- 타겟 (Target)
  - 어드바이스를 받는 객체, 포인트 컷으로 결정
- 어드바이스 (Advice)
  - 부가 기능으로 특정 조인 포인트에서 Aspect에 의해 취해지는 조치
  - Around, Before, After 와 같은 다양한 어드바이스가 있음 
- 애스팩트 (Aspect)
  - 어드바이스 + 포인트 컷을 모듈화 한것으로 @Aspect를 생각하면 된다.
  - 여러 어드바이스와 포인트 컷이 함께 존재
- 위빙 (Weaving)
  - 포인트 컷으로 결정된 타겟의 조인 포인트에 어드바이스를 적용 하는 것
  - 위빙을 통해 핵심 기능 코드에 영향을 주지 않고 부가 기능을 추가 할 수 있음
  - AOP적용을 위해 애스펙트를 객체에 연결한 상태
    - 컴파일 타임(AspectJ compiler)
    - 로드타임 , 런타임, 스프링 AOP는 런타임, 프록시 방식
- AOP 프록시
  - AOP기능을 구현하기 위해 만든 프록시 객체, 스프링에서 AOP프록시는 JDK동적 프록시 또는 CGLIB 프록시 이다.

## 10. 스프링 AOP 구현

build.gradle에 다음을 추가 
```console
implementation 'org.springframework.boot:spring-boot-starter-aop'
```
@Aspect를 사용하려면 @EnableAspectJAutoProxy를 스프링 설정에 추가해야 하지만 스프링 부트를 사용하는 경우는 자동으로 추가된다.


### 스프링 AOP 구현 1

> @Aspect를 포함한 org.aspectj 패키지 관련기능은 aspectweaver.jar라이브러리가 제공하는 기능이다.
> 
> 스프링에서는 AOP기능과 함께 aspectweaver.jar도 함께 사용할 수 있게 의존관계에도 포함된다.
> 
> 애노테이션이나 관련 인터페이스만 사용하는 것이라서 실제 AspectJ가 제공하는 컴파일 로드타임 위버를 사용하는 것은 아니다.
> 
> 스프링은 지금까지 구현했던것 처럼 프록시 방식의 AOP를 사용한다.

@Aspect는 애스팩트라는 표식이지 컴포넌트 스캔이 동작하지 않아서 AspectV1을 사용하려면 빈으로 등록해야한다.

스프링 빈으로 사용하는 방법은 다음과 같다.

1. @Bean : 을 사용해서 직접 등록
2. @Component : 컴포넌트 스캔을 사용해서 자동 등록
3. @Import : 주로 설정파일을 추가할 때 사용 (@Configuration)

### 스프링 AOP 구현 2 - 포인트컷 분리

@Around 에 포인트컷 표현식을 직접 넣을 수 도 있지만 @Pointcut 애노테이션을 사용해서 별도로 분리할 수 있다.

   * 포인트컷 시그니처 : 메서드 이름과 파라미터를 합친것으로 주문과 관련된 모든 기능을 대상으로 하는 포인트 컷이다.
   * 반환 타입은 void로 코드의 내용은 비워둬야 한다.
   * 다른 애스팩트에서 참고하려면 public을 사용하고 그렇지 않은경우는 private를 사용한다.

### 스프링 AOP 구현 3 - 어드바이스 추가

> 로그를 출력하는 기능에 추가로 단순 트랜잭션을 적용하는 코드를 추가할 것이다.
> 트랜잭션 기능은 보통 다음과 같이 동작한다.

 - 핵심 로직 실행 직전에 트랜잭션 시작
 - 핵심 로직 실행
 - 핵심 로직 실행에 문제가 없으면 커밋
 - 핵심 로직 실행에 예외바 발생하면 롤백
 
포인트컷이 적용된 AOP는 다음과 같다.
- orderService: doLog(), doTransaction() 어드바이스 적용
- orderRepository: doLog() 어드바이스 적용

> AOP 적용 전

클라이언트 -> orderSerivce.orderItem() -> orderRepository.save()

> AOP 적용 후

클라이언트 -> doLog() -> doTransaction() 
  -> orderService.orderItem()
-> doLog() -> orderRepository.save()

### 스프링 AOP 구현 4 - 포인트 컷 참조

> 포인트 컷을 공용으로 사용하기 위해 외부 클래스에 모아두고 외부에서 호출시에 접근제어자를 public으로 선언후 적용한다.

기존 allOrder() allServer() 를 orderAndService()로 포인트컷을 만든다.

포인트 컷을 여러 어드바이스에서 사용할때 이 방법을 사용하면 효과적이다.

### 스프링 AOP 구현 5 - 어드바이스 순서

어드바이스는 기본적으로 순서를 보장하지 않기 때문에 순서를 지정할 경우엔 
@Aspect 적용 단위로 org.springframework.core.annotation.@Order 애노테이션을 적용해야 한다.
하지만 어드바이스 단위가 아닌 클래스 단위로 적용되기 때문에 지금처럼 하나의 애스펙트에 여러 어드바이스가 있으면 순서보장이 안된다.

따라서 애스펙트를 별도의 클래스로 분리해야한다.

적용전 로그를 남기는 순서 : doLog() -> doTransaction()
적용후 로그를 남기는 순서 : doTransaction() -> doLog()


### 스프링 AOP 구현 6 - 어드바이스 종류

> 어드바이스 종류

- @Around : 메서드 호출 전후에 수행, 가장 강력한 어드바이스, 조인 포인트 실행 여부 선택, 반환 값 변환, 예외 변환 등이 가능
  - @Around 와 다르게 작업 흐름을 변경할 수는 없다
  - ProceedingJoinPoint.proceed() 를 호출해야 다음 대상이 호출된다.
- @Before : 조인 포인트 실행 이전에 실행
  - ProceedingJoinPoint.proceed() 자체를 사용하지 않는다. 
  - 메서드 종료시 자동으로 다음 타켓이 호출된다. 
  - 물론 예외가 발생하면 다음 코드가 호출되지는 않는다.
- @AfterReturning : 조인 포인트가 정상 완료후 실행 
  - returning 속성에 사용된 이름은 어드바이스 메서드의 매개변수 이름과 일치해야 한다.
  - returning 절에 지정된 타입의 값을 반환하는 메서드만 대상으로 실행한다. (부모 타입을 지정하면 모든 자식 타입은 인정된다.)
  - 반환되는 객체를 변경할 수 없다. 변경하려면 @Around를 사용해야한다.
- @AfterThrowing : 메서드가 예외를 던지는 경우 실행
  - throwing 속성에 사용된 이름은 어드바이스 메서드의 매개변수 이름과 일치해야 한다.
  - throwing 절에 지정된 타입과 맞은 예외를 대상으로 실행한다. (부모 타입을 지정하면 모든 자식 타입은 인정된다.)
- @After : 조인 포인트가 정상 또는 예외에 관계없이 실행(finally)
  - 메서드 실행이 종료되면 실행된다. (finally를 생각하면 된다.)
  - 정상 및 예외 반환 조건을 모두 처리한다.
  - 일반적으로 리소스를 해제하는 데 사용한다.

> 조인포인트 인터페이스의 주요 기능

- getArgs() : 메서드 인수를 반환
- getThis() : 프록시 객체를 반환
- getTarget() : 대상 객체를 반환
- getSignature() : 조언되는 메서드에 대한 설명을 반환
- toString() : 조언되는 방법에 대한 유용한 설명을 인쇄

> ProceedingJoinPoint 인터페이스의 주요 기능

- proceed() : 다음 어드바이스나 타켓을 호출

> 실행결과는 다음과 같다.

```console
[around][트랜잭션 시작] void me.chulgil.spring.aop.order.OrderService.orderItem(String)
[before] void me.chulgil.spring.aop.order.OrderService.orderItem(String)
[orderService] 실행
[orderRepository] 실행
[return] void me.chulgil.spring.aop.order.OrderService.orderItem(String) return=null
[after] void me.chulgil.spring.aop.order.OrderService.orderItem(String)
[around][트랜잭션 커밋] void me.chulgil.spring.aop.order.OrderService.orderItem(String)
[around][리소스 릴리즈] void me.chulgil.spring.aop.order.OrderService.orderItem(String)
```

> 순서 

- 스프링은 5.2.7 버전부터 동일한 @Aspect 안에서 동일한 조인포인트의 우선순위를 정했다.
- 실행 순서: @Around , @Before , @After , @AfterReturning , @AfterThrowing
- 어드바이스가 적용되는 순서는 이렇게 적용되지만, 호출 순서와 리턴 순서는 반대다.
- @Aspect 안에 동일한 종류의 어드바이스가 2개 있으면 순서가 보장되지 않는다. 이 경우 앞서 배운 것 처럼 @Aspect 를 분리하고 @Order 를 적용하자.


@Around 하나만 있어도 모든 기능을 수행 할 수 있지만 다른 어드바이스들이 존재하는 이유는 무엇일까?

@Around는 항상 jointPoint.proceed()를 호출해야한다. 만약 실수로 호출하지 않았다면 타겟이 호출되지 않는 치명적인 버그가 발생한다.

@Around는 가장 넓은 기능을 제공하지만 실수할 가능성이 있다. 반면 @Before @After는 기능은 적지만 실수할 가능성이 낮고 코드도 단순하다.
그리고 작성한 의도가 명확하게 들어난다.

> 좋은 설계는 제약이 있는 것이다. 제약은 실수를 방지하고 일종의 가이드 역할을 한다.
> 
> 만약 @Around를 사용했는데 중간에 다른 개발자가 해당 코드를 수정해서 호출하지 않았다면 큰 장애가 발생했을 것이다.
> 
> 처음부터 @Before를 사용했다면 이런 문제 자체가 발생하지 않는다.
> 
> 제약 덕분에 역할이 명확해져서 코드를 보고 고민해야 하는 범위가 줄어들고 코드의 의도도 파악하기 쉽다.

## 11. 스프링 AOP - 포인트 컷

> 애스펙트J는 포인트컷을 편리하게 표현하기 위한 특별한 표현식을 제공한다.
> 
> 예) @Pointcut("execution(* hello.aop.order..*(..))")
> 
> 포인트컷 표현식은 AspectJ pointcut expression 즉 애스펙트J가 제공하는 포인트컷 표현식을 줄여서 말하는 것이다.


> 포인트컷 지시자의 종류

- execution : 메소드 실행 조인 포인트를 매칭한다. 스프링 AOP에서 가장 많이 사용하고, 기능도 복잡하다.
- within : 특정 타입 내의 조인 포인트를 매칭한다.
- args : 인자가 주어진 타입의 인스턴스인 조인 포인트
- this : 스프링 빈 객체(스프링 AOP 프록시)를 대상으로 하는 조인 포인트
- target : Target 객체(스프링 AOP 프록시가 가르키는 실제 대상)를 대상으로 하는 조인 포인트 
- @target : 실행 객체의 클래스에 주어진 타입의 애노테이션이 있는 조인 포인트
- @within : 주어진 애노테이션이 있는 타입 내 조인 포인트
- @annotation : 메서드가 주어진 애노테이션을 가지고 있는 조인 포인트를 매칭
- @args : 전달된 실제 인수의 런타임 타입이 주어진 타입의 애노테이션을 갖는 조인 포인트 
- bean : 스프링 전용 포인트컷 지시자, 빈의 이름으로 포인트컷을 지정한다.

### 스프링 AOP - 포인트 컷 종류

> Execution 파라미터 매칭 규칙은 다음과 같다.
- (String) : 정확하게 String 타입 파라미터
- () : 파라미터가 없어야 한다.
- (*) : 정확히 하나의 파라미터, 단 모든 타입을 허용한다.
- (*, *) : 정확히 두 개의 파라미터, 단 모든 타입을 허용한다.
- (..) : 숫자와 무관하게 모든 파라미터, 모든 타입을 허용한다. 참고로 파라미터가 없어도 된다. 0..* 로 이해하면 된다.
- (String, ..) : String 타입으로 시작해야 한다. 숫자와 무관하게 모든 파라미터, 모든 타입을 허용한다.
  - 예) (String) , (String, Xxx) , (String, Xxx, Xxx) 허용

> execution과 args의 차이점
- execution 은 클래스에 선언된 정보를 기반으로 판단 -> execution 은 파라미터 타입이 정확하게 매칭되어야 한다.
- args 는 실제 넘어온 파라미터 객체 인스턴스를 보고 판단 -> args 는 부모 타입을 허용한다.

> @target : 실행 객체의 클래스에 주어진 타입의 애노테이션이 있는 조인 포인트 -> 부모클래스의 메서드까지 어드바이스 적용
> 
> @within : 주어진 애노테이션이 있는 타입 내 조인 포인트 -> 자기자신의 클래시에 정의된 메서드에만 어드바이스 적용
>
> @annotation : 메서드가 주어진 애노테이션을 가지고 있는 조인 포인트를 매칭
> 
> @args : 전달된 실제 인수의 런타임 타입이 주어진 타입의 애노테이션을 갖는 조인 포인트
> 
> bean : 스프링 전용 포인트컷 지시자, 빈의 이름으로 지정한다.

다음은 포인트컷 표현식을 사용해서 어드바이스에 매개변수를 전달할 수 있다.
this, target, args,@target, @within, @annotation, @args

```java
@Before("allMember() && args(arg,..)")
  public void logArgs3(String arg) {
      log.info("[logArgs3] arg={}", arg);
  }
```
추가로 타입이 메서드에 지정한 타입으로 제한된다. 
여기서는 메서드의 타입이 String 으로 되어 있기 때문에 다음과 같이 정의되는 것으로 이해하면 된다.

> this : 스프링 빈 객체(스프링 AOP 프록시)를 대상으로 하는 조인 포인트
> target : Target 객체(스프링 AOP 프록시가 가르키는 실제 대상)를 대상으로 하는 조인 포인트

> 설명
- this , target 은 다음과 같이 적용 타입 하나를 정확하게 지정해야 한다. 
- this(me.chulgil.spring.aop.member.IMemberService)
- target(me.chulgil.spring..aop.member.MemberService) 
- 둘다 * 같은 패턴을 사용할 수 없다.
- 둘다 부모 타입을 허용한다. 

> this vs target

단순히 타입 하나를 정하면 되는데, this 와 target 은 어떤 차이가 있을까?
스프링에서 AOP를 적용하면 실제 target 객체 대신에 프록시 객체가 스프링 빈으로 등록된다. 
- this 는 스프링 빈으로 등록되어 있는 프록시 객체를 대상으로 포인트컷을 매칭한다.
- target 은 실제 target 객체를 대상으로 포인트컷을 매칭한다.
 
결국 프록시 생성 방식에 따른 차이가 있다.

- JDK 동적 프록시: 인터페이스가 필수이고, 인터페이스를 구현한 프록시 객체를 생성한다.
- CGLIB: 인터페이스가 있어도 구체 클래스를 상속 받아서 프록시 객체를 생성한다.

> 프록시를 대상으로 하는 this의 경우 구체 클래스를 지정하면 프록시 생성 전략에 따라서 다른 결과가 나올 수 있다.
> this, target 지시자는 단독으로 사용되기 보다는 파라미터 바인딩에서 주로 사용된다.

## 12. 스프링 AOP - 실전 예제

> 스프링 AOP 실전 예제 - 로그출력 
 - @Trace 애노테이션으로 로그 출력하기
> 스프링 AOP 실전 예제 - 재시도 
 - @Retry 애노테이션으로 예외 발생시 재시도 하기

 5번에 1번정도 실패하는 저장소가 있다면 실패할 경우 재시도 하는 AOP가 있으면 편리하다.


## 13. 스프링 AOP - 주의사항

### 프록시와 내부 호출 문제


스프링은 프록시 방식의 AOP를 사용하기 때문에 항상 프록시를 통해서 대상 객체(Target)을 호출해야 한다.
만약 프록시를 거치지 않고 직접 대상을 호출하게 되면 AOP가 적용되지 않고 어드바이스도 호출 되지 않는다.

AOP를 적용하려면 항상 객체 대신에 프록시를 스프링 빈으로 등록해야 하는데 스프링의 경우 의존관계 주입시 
항상 프록시 객체를 주입하고 있어서 별 문제가 되지 않는다.
하지만 객체 내부에서 메서드 호출이 발생하면 프록시를 거치지 않기 때문에 문제가 발생하게 된다.

> 아래와 같은 경우 internal에 대해서는 AOP적용이 되지 않는다.
```java
public void external() {
    this.internal(); 
}
public void internal() {}
```

> 대안1 : 자기 자신 주입

아래 callServiceV1은 실제 자신이 아니라 주입받은 프록시 객체이다. 
생성자 주입의 경우는 본인을 생성하면서 주입하기때문에 순환 사이클 오류가 발생하기 때문에 set으로 주입하도록 한다.

```java
private CallServiceV1 callServiceV1;
@Autowired
public void setCallServiceV1(CallServiceV1 callServiceV1) {
    this.callServiceV1 = callServiceV1;
}

public void external() {
    callServiceV1.internal();
}
```


참고 : 스프링 버전이2.6.0 이상인 경우 순환참조 기능이 디폴트로 사용하지 않음으로 되어있음으로 아래와 같이 프로퍼티에 설정해 주어야 동작한다.
```console
spring.main.allow-circular-references=true
```

### 대안2 : 지연 조회

> 스프링 빈을 지연해서 조회하는 방법으로 ObjectProvider(Provider), ApplicationContext를 사용한다.

ObjectProvider는 스프링 컨테이너에서 조회하는 것을 빈 생성 시점이 아니라 실제 객체를 사용하는 시점으로 지연할 수 있다.
아라서 callServiceProvider.getObject()를 호출하는 시점에 빈을 조회한다.
또한 자기 자신을 주입 받는 것이 아니기 때문에 순환 사이클이 발생하지 않는다.

```java
private final ObjectProvider<CallServiceV2> callServiceProvider;

public void external() {
    CallServiceV2 callServiceV2 = callServiceProvider.getObject();
    callServiceV2.internal();
}

public void internal() {}
```

### 대안3 : 구조 변경

가장 나은 대안은 내부 호출이 발생하지 않도록 구조를 변경하는 방법으로 이 방법이 가장 권장된다.

AOP는 주로 트랜잭션 적용이나 주요 컴포넌트의 로그 출력 기능에 사용된다.
인터페이스에 메서드가 나올정도의 규모에 AOP를 사용하는 것이 적당하다.
즉 public메서드에만 적용하고 private메서드 처럼 작은 단위에는 AOP를 적용하지 않는다.


### 프록시 기술과 한계 - 타입 캐스팅

인터페이스 사용시 JDK동적 프록시를 사용하고 구체 클래스 사용시에는 CGLIB를 사용하는데 아래 옵션으로 프록시 생성을 유연하게 사용할 수 있다.

- proxyTargetClass=false : JDK동적 프록시 사용으로 인터페이스 기반 프록시 생성
- proxyTargetClass=true : CGLIB 사용으로 구체 클래스 기반 프록시 생성
- 옵션이 없는경우 인터페이스가 없다면 CGLIB를 자동으로 사용


> 인터페이스 기반 프록시생성의 경우 구체 클래스로 타입 캐스팅이 불가능한 한계가 있다.
>
> CGLIB를 사용하면 위에서 언급한 문제가 발생하지는 않지만 아래 단점 또한 존재한다.

1. 대상 클래스에 기본 생성자 필수
  - 구체 클래스를 상속받음으로 자식클래스에서 부모클래스의 생성자를 호출해야 한다.
  - CGLIB프록시가 대상 클래스를 상속받고 대상 클래스의 기본생성자를 호출하기 때문에 대상 클래스에 기본생성자를 만들어야 한다.
2. 생성자 2번 호출 문제
  - 실제 target의 객체를 생성할 때 한번 프록시 객체를 생성시 부모 클래스의 생성자 호출할때 두번 호출된다.
3. final 키워드 클래스, 메서드 사용불가
  - final 키워드가 클래스에 있으면 상속이 불가능하고, 메스드에 있으면 오버라이딩이 불가능하다.
  - CGLIB는 상속을 기반으로 하기 때문에 위 두 경우는 프록시가 생성되지 않거나 정상 동작하지 않는다.

프레임워크 같은 개발이 아니라 일반적인 웹 어플리케이션을 개발할 때는 final 키워드를 잘 사용하지 않기 때문에 큰 문제는 되지 않는다.

### 프록시 기술과 한계 - 스프링의 해결

> 스프링의 기술 선택 변화

1. 스프링 3.2 CGLIB를 스프링 내부에 함께 패키징
2. 스프링 4.0 부터 CGLIB의 기본 생성자 문제가 해결됨
  - objenesis 라는 특별한 라이브러리를 사용해서 기본 생성자 없이 객체 생성이 가능하다. 
3. CGLIB의 생성자 2번 호출문제가 해결됨
  - objenesis 라이브러리로 생성자가 1번만 호출됨
4. 스프링 부트 2.0 버전 부터 CGLIB를 기본으로 사용하도록 채택
  - 별도의 설정이 없다면 AOP를 사용할때 기본적으로 proxyTargetClass=true 로 설정해서 사용됨

> 여전히 남은 문제라면 AOP를 적용할 대상에 final 클래스/메서드 는 사용할 수 없다.
> 
> 하지만 자주 사용되지 않기 때문에 큰 문제는 없다.


