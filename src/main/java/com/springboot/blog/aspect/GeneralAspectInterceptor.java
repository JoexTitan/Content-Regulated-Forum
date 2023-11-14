package com.springboot.blog.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class GeneralAspectInterceptor {

    Logger logger= LoggerFactory.getLogger(GeneralAspectInterceptor.class);

    @Around("@annotation(com.springboot.blog.aspect.GetExecutionTime)")
    public Object trackTime(ProceedingJoinPoint currOperation) throws Throwable {
        long stratTime = System.currentTimeMillis();
        Object obj = currOperation.proceed();
        long endTime = System.currentTimeMillis();
        logger.info("Method name" + currOperation.getSignature() +
                " time taken to execute : " + (endTime-stratTime));
        return obj;
    }

    @Pointcut(value="execution(* com.springboot.blog.controller.*.*(..) )")
    public void generalGlobalPointcut() {
    }

    @Around("generalGlobalPointcut()")
    public Object applicationLogger(ProceedingJoinPoint pjp) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().toString();
        Object[] array = pjp.getArgs();
        log.info("method invoked " + className + " : " + methodName + "()" + "arguments : "
                + mapper.writeValueAsString(array));
        Object object = pjp.proceed();
        log.info(className + " : " + methodName + "()" + "Response : "
                + mapper.writeValueAsString(object));
        return object;
    }
}
