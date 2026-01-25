/**
 * @File        : ChatSessionServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of ChatSession Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service
public class ChatSessionServiceImpl implements ChatSessionService {

  @PostConstruct public void init() {
    log.trace("INIT:{}", ChatSessionService.class);
  }

  @Override public List<ChatSession> chatSessionList() {
    return List.of(
      ChatSession.builder()
        .name("tester")
        .intro("hello!")
        .members(List.of("/images/test.svg"))
        .active(true)
        .online(true)
        .updated("PM 01:10")
        .unread(0)
      .build()
    );
  }
}
