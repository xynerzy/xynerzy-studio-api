/**
 * @File        : MainAspect.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Aspect
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component @Aspect @Slf4j
public class MainAspect {
  // @Around("(execution(* com.xynerzy..*ServiceImpl.*(..)))")
  @Around("(execution(* com.xynerzy.main.service.MainServiceImpl.mainTest(..)))")
  public Object mainAspect(ProceedingJoinPoint jp) throws Throwable {
    MethodSignature sig = (MethodSignature) jp.getSignature();
    log.debug("MAIN-ASPECT:{}.{}", jp.getTarget().getClass().getName(), sig.getMethod().getName());
    return jp.proceed();
  }
}
