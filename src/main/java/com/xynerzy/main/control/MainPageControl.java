/**
 * @File        : MainPageControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Page Control
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.xynerzy.main.service.MainService;
import com.xynerzy.system.runtime.AppException;
import com.xynerzy.system.runtime.Settings;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Controller @RequestMapping @RequiredArgsConstructor
public class MainPageControl {
  static final String CONTROLLER_TAG1 = "Main page"; 

  private final MainService mainService;

  private final Settings settings;

  @PostConstruct public void init() {
    log.trace("INIT:{}", MainPageControl.class, mainService);
  }

  @Operation(summary = "Main page", tags = { CONTROLLER_TAG1 })
  @GetMapping(path = { "/", "/main" })
  public void mainPage() throws AppException {
    log.debug("CHECK:{}", settings.getAppName());
    mainService.main();
  }

  @GetMapping("/error")
  public ModelAndView errorPage(ModelAndView mav, Exception err) throws AppException {
    log.debug("CHECK:{}", err);
    mav.setViewName("/index.html");
    return mav;
  }
}
