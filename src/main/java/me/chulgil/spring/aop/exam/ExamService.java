package me.chulgil.spring.aop.exam;

import lombok.RequiredArgsConstructor;
import me.chulgil.spring.aop.exam.annotation.Retry;
import me.chulgil.spring.aop.exam.annotation.Trace;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository excemRepository;

    @Trace
    @Retry(4)
    public void request(String itemId) {
        excemRepository.save(itemId);
    }
}
