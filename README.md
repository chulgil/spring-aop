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
