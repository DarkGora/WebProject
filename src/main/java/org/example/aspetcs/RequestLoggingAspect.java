package org.example.aspetcs;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RequestLoggingAspect {

    // Точеки среза для всех методов в пакете restcontrollers
    @Pointcut("execution(* org.example.restcontroller.*.*(..))")
    public void controllerMethods() {}


    @Around("controllerMethods()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        long startTime = System.currentTimeMillis();

        // Логирование входящего запроса
        log.info("=== INCOMING REQUEST ===");
        log.info("URL: {} {}", request.getMethod(), request.getRequestURL());
        log.info("IP: {}", request.getRemoteAddr());
        log.info("Controller: {}", joinPoint.getTarget().getClass().getSimpleName());
        log.info("Method: {}", ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
        log.info("Arguments: {}", Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Логирование успешного выполнения
            log.info("=== REQUEST COMPLETED ===");
            log.info("Execution time: {} ms", executionTime);
            log.info("Status: SUCCESS");

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Логирование ошибки
            log.error("=== REQUEST FAILED ===");
            log.error("Execution time: {} ms", executionTime);
            log.error("Error: {}", e.getMessage());
            log.error("Exception type: {}", e.getClass().getSimpleName());

            throw e;
        }
    }
}