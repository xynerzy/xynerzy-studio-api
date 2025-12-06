/**
 * @File        : MainPageControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Page Control
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.main.control;

import static com.xynerzy.commons.WebUtil.currentRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.xynerzy.main.service.MainService;
import com.xynerzy.system.runtime.AppException;
import com.xynerzy.system.runtime.Settings;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;


@Slf4j @Controller @RequestMapping
public class MainPageControl {
  static final String CONTROLLER_TAG1 = "Main page"; 

  @Autowired private MainService mainService;

  @Autowired private Settings settings;

  @PostConstruct public void init() {
    log.trace("INIT:{}", MainPageControl.class, mainService);
  }

  @Operation(summary = "Main page", tags = { CONTROLLER_TAG1 })
  @GetMapping(path = { "/", "/main" })
  public ModelAndView mainPage(ModelAndView mav) throws AppException {
    HttpSession ss = currentRequest().getSession();
    log.debug("CHECK:{}", settings.getAppName());
    if (ss != null) {
      log.debug("CHECK-SESSION:{}", ss.getId());
      Object t = ss.getAttribute("TEST");
      if (t == null) {
        ss.setAttribute("TEST", t = 1);
      } else {
        Integer n = (Integer) t;
        ss.setAttribute("TEST", t = (n + 1));
      }
      log.debug("T:{}", t);
    }
    mainService.main();
    mav.setViewName("/index.html");
    return mav;
  }

  @GetMapping("/error")
  public ModelAndView errorPage(ModelAndView mav, Exception err) throws AppException {
    log.debug("CHECK:{}", err);
    mav.setViewName("/index.html");
    return mav;
  }
}
