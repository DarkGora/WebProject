package org.example.aspetcs;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Точеки среза для всех методов в пакете services
    @Pointcut("execution(* org.example.service.*.*(..))")
    public void serviceMethods() {
    }


    // Точеки среза для методов с аннотацией SimpleLog
    @Pointcut("@annotation(org.example.annota.SImplelog)")
    public void simpleLogMethods() {
    }


    @Around("serviceMethods() || simpleLogMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("Вызов метода: {}.{}() с аргументами: {}",
                className, methodName, joinPoint.getArgs());

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Метод: {}.{}() выполнен за {} мс. Результат: {}",
                    className, methodName, executionTime, result);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("Ошибка в методе: {}.{}() за {} мс. Исключение: {}",
                    className, methodName, executionTime, e.getMessage(), e);
            throw e;
        }
    }
}