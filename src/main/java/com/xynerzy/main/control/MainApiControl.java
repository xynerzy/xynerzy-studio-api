/**
 * @File        : MainApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main API Control
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.control;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.main.service.MainService;
import com.xynerzy.system.runtime.AppException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api")
public class MainApiControl {
  static final String CONTROLLER_TAG1 = "Main page API"; 

  private final MainService mainService;

  @PostConstruct public void init() {
    log.trace("INIT:{}", MainApiControl.class, mainService);
  }

  @Operation(summary = "PING API", tags = { CONTROLLER_TAG1 })
  @RequestMapping(path = "/ping", method = RequestMethod.HEAD)
  public void ping() throws AppException {
    mainService.ping();
  }

  @Operation(summary = "Main API", tags = { CONTROLLER_TAG1 })
  @GetMapping(path = { "/main" }) @ResponseBody
  public Object main() throws AppException {
    return mainService.main();
  }

  @Operation(summary = "SockJS + STOMP WebSocket Endpoint", tags = { CONTROLLER_TAG1 }, description = """
    SockJS endpoint for real-time messaging.
    STOMP CONNECT:
    - endpoint: /api/ws
    - protocol: STOMP
    SUBSCRIBE:
    - /api/sub/{topic}
    SEND:
    - /api/pub
    """)
  @PostMapping("/ws") public void sockJsDocOnly() { }
}
