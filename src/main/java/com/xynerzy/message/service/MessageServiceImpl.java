/**
 * @File        : MessageServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.service;

import java.util.List;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import com.xynerzy.message.entity.MessageEntity.Message;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service
public class MessageServiceImpl implements MessageService {
  
  @PostConstruct public void init() {
    log.trace("INIT:{}", MessageService.class);
  }
  
  @Override public List<Message> messageList() {
    return List.of(
      Message.builder()
        .type("my")
        .content("Hi! whatsup!?")
        .time("PM 01:10")
        .userId("tester")
        .unread(1)
      .build(),
      Message.builder()
        .type("their")
        .content("Nothing special. How about you?")
        .avatar("/images/test.svg")
        .time("PM 01:10")
        .userId("tester")
        .unread(1)
      .build()
    );
  }

  @Override public void messageList(org.springframework.messaging.Message<Message> msg, MessageHeaders hdr,
      StompHeaderAccessor acc) {
  }
}
