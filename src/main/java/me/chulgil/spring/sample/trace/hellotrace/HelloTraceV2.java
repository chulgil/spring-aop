package me.chulgil.spring.sample.trace.hellotrace;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.sample.trace.TraceId;
import me.chulgil.spring.sample.trace.TraceStatus;
import org.springframework.stereotype.Component;

/**
 *
 */
@Slf4j
@Component
public class HelloTraceV2 {

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    /**
     * 로그 시작 : 로그 메시지를 파라미터로 받아서 로그를 출력
     * @param message
     * @return TraceStatus
     */
    public TraceStatus begin(String message) {
        TraceId traceId = new TraceId();
        Long startTimeMs = System.currentTimeMillis();
        log.info("[" + traceId.getId() + "] " + addSpace(START_PREFIX, traceId.getLevel()) + message);
        return new TraceStatus(traceId, startTimeMs, message);
    }

    /**
     * 로그 종료 : 상태 값으로 실행 시간을 계산 후 종료시 출력
     * @param status
     */
    public void end(TraceStatus status) {
        complete(status, null);
    }

    /**
     * 예외 종료 : 로그를 예외 상황 으로 종료
     * @param status
     * @param e
     */
    public void exception(TraceStatus status, Exception e) {
        complete(status, e);
    }

    private void complete(TraceStatus status, Exception e) {
        Long stopTimeMs = System.currentTimeMillis();
        long resultTimeMs = stopTimeMs - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();
        if (e == null) {
            log.info("[{}] {}{} time={}ms", traceId.getId(), addSpace(COMPLETE_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs);
        } else {
            log.info("[{}] {}{} time={}ms ex={}", traceId.getId(), addSpace(EX_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs, e.toString());
        }
    }

    private static String addSpace(String prefix, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append( (i == level - 1) ? "|" + prefix : "|   ");
        }
        return sb.toString();
    }

    /**
     * 파라미터 동기화 시작
     * @param beforeTraceId
     * @param message
     * @return
     */
    public TraceStatus beginSync(TraceId beforeTraceId, String message) {
        TraceId nextId = beforeTraceId.createNextId();
        Long startTimeMs = System.currentTimeMillis();
        log.info("[" + nextId.getId() + "] " + addSpace(START_PREFIX, nextId.getLevel()) + message);
        return new TraceStatus(nextId, startTimeMs, message);
    }

}
