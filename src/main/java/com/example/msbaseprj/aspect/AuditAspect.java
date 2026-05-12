package com.example.msbaseprj.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AuditAspect {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        logger.info("Audit action: " + auditable.action());
        return joinPoint.proceed();
    }

}
