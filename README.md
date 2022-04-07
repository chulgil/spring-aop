# spring-aop

## 로그추적기

### 요구사항 분석

애플리케이션이 커지면서 어떤 부분에서 병목과 예외가 발생하는지 로그를 통해 확인하고자 한다.
기존에는 문제 발생시 관련부분을 어렵게 찾아서 로그를 남겼지만 이부분을 개선하고 자동화하는 것이 요구사항이다.

### 요구사항

- 모든 Public 메서드의 호출과 응답 정보를 로그로 출력
- 애플리케이션의 흐름을 변경하면 안됨
- 로그를 남긴다고 해서 비지니스 로직의 동작에 영향을 주면 안됨
- 메서드 호출에 걸린시간 표기
- 정상 / 예외 흐름 구분하기 : 예외 발생시 정보 표기
- 메서드 호출의 깊이 표현
- HTTP 요청을 구분
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

```console
curl http://localhost:8080/v1/request\?itemId\=hello
```

```console
2022-04-07 18:06:58.977  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [ed67e946] OrderController.request()
2022-04-07 18:06:58.978  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [97bdb531] OrderService.orderItem()
2022-04-07 18:06:58.978  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [21f39438] OrderRepository.save()
2022-04-07 18:06:59.978  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [21f39438] OrderRepository.save() time=1000ms
2022-04-07 18:06:59.979  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [97bdb531] OrderService.orderItem() time=1001ms
2022-04-07 18:06:59.979  INFO 87714 --- [nio-8080-exec-1] m.c.s.s.trace.hellotrace.HelloTraceV1    : [ed67e946] OrderController.request() time=1002ms
```