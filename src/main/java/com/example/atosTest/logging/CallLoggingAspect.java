package com.example.atosTest.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Aspect
@Component
public class CallLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(CallLoggingAspect.class);

    @Around("within(com.example.atosTest..*) "
            + "&& @within(org.springframework.web.bind.annotation.RestController)")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        log.info("--> {} input={}", method, Arrays.toString(joinPoint.getArgs()));
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.info("<-- {} output={} duration={}ms", method, result, elapsedMillis(start));
            return result;
        } catch (Throwable ex) {
            log.warn("<-- {} threw {}: {} duration={}ms",
                    method, ex.getClass().getSimpleName(), ex.getMessage(), elapsedMillis(start));
            throw ex;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
