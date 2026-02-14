/**
 * @File        : MainServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Service Implementation
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.service;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static org.springframework.aop.framework.AopContext.currentProxy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.xynerzy.commons.SimplePublisherSubscribers;
import com.xynerzy.system.runtime.AppException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class MainServiceImpl implements MainService {

  private final SimplePublisherSubscribers<Object> pubsub = new SimplePublisherSubscribers<>();

  private final SimpMessagingTemplate wsock;

  @PostConstruct public void init() {
    log.trace("INIT:{}", MainService.class);
  }

  @Override public Object main() throws AppException {
    Map<String, Object> ret = new LinkedHashMap<>();
    cast(currentProxy(), this).mainTest();
    return ret;
  }

  public void mainTest() throws AppException {
    log.debug("MAIN-TEST");
  }

  @Override public Object subscribe(String topic) throws AppException {
    return pubsub.subscribe(topic, 1000 * 30, "{}");
  }

  @Override public Object publish(String topic, Map<String, Object> prm) throws AppException {
    pubsub.publish(null, topic, prm);
    return "{}";
  }

  @Override public void handleSubscribeEvent(SessionSubscribeEvent event) {
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    Map<String, Object> atr = sha.getSessionAttributes();
    // String destination = sha.getDestination();
    // String sid = sha.getSessionId();
    // String subscId = concat(sid, sha.getSubscriptionId());
    // String userId = null;
    wsock.convertAndSend("/api/sub", "OK");
    log.debug("SESSION:{} / {}", atr, sha);
  }
}
