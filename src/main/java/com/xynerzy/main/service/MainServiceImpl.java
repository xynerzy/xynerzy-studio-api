/**
 * @File        : MainServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Service Implementation
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.service;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.WebUtil.currentRequest;
import static com.xynerzy.commons.WebUtil.currentResponse;
import static org.springframework.aop.framework.AopContext.currentProxy;

import org.springframework.stereotype.Service;

import com.xynerzy.system.runtime.AppException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class MainServiceImpl implements MainService {

  @PostConstruct public void init() {
    log.trace("INIT:{}", MainService.class);
  }

  @Override public Object main() throws AppException {
    cast(currentProxy(), this).mainTest();
    currentRequest();
    currentResponse();
    return null;
  }

  public void mainTest() throws AppException {
    log.debug("MAIN-TEST");
  }
}
