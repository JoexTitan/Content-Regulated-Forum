package com.springboot.blog.aspect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;

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
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().toString();
        Object[] array = pjp.getArgs();

        // Log method invocation with arguments
        log.info("method invoked " + className + " : " + methodName + "()" + "arguments : "
                + getJsonString(array, mapper));

        Object object = pjp.proceed();

        // Log method response
        log.info(className + " : " + methodName + "()" + "Response : "
                + getJsonString(object, mapper));

        return object;
    }

    private String getJsonString(Object obj, ObjectMapper mapper) {
        try {
            // Checking if the object is serializable
            if (obj != null) {
                return mapper.writeValueAsString(obj);
            } else {
                return "null";
            }
        } catch (JsonProcessingException e) {
            log.warn("Error processing JSON for object of type {}: {}", obj != null ? obj.getClass().getName() : "null", e.getMessage());
            return "Error processing JSON";
        }
    }

//    @Around("generalGlobalPointcut()")
//    public Object applicationLogger(ProceedingJoinPoint pjp) throws Throwable {
//        ObjectMapper mapper = new ObjectMapper();
//        String methodName = pjp.getSignature().getName();
//        String className = pjp.getTarget().getClass().toString();
//        Object[] array = pjp.getArgs();
//        log.info("method invoked " + className + " : " + methodName + "()" + "arguments : "
//                + mapper.writeValueAsString(array));
//        Object object = pjp.proceed();
//        log.info(className + " : " + methodName + "()" + "Response : "
//                + mapper.writeValueAsString(object));
//        return object;
//    }
}
