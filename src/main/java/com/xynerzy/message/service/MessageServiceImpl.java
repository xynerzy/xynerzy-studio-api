/**
 * @File        : MessageServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.service;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import com.xynerzy.message.entity.MessageEntity;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
  
  private final SimpMessagingTemplate wsock;
  
  @PostConstruct public void init() {
    log.trace("INIT:{}", MessageService.class);
  }
  
  // @Override public List<MessageEntity.Message> messageList() {
  //   return List.of(
  //     MessageEntity.Message.builder()
  //       .type("my")
  //       .content("Hi! whatsup!?")
  //       .time("PM 01:10")
  //       .userId("tester")
  //       .unread(1)
  //     .build(),
  //     MessageEntity.Message.builder()
  //       .type("their")
  //       .content("Nothing special. How about you?")
  //       .avatar("/images/test.svg")
  //       .time("PM 01:10")
  //       .userId("tester")
  //       .unread(1)
  //     .build()
  //   );
  // }

  @Override public List<MessageEntity.Message> messageList(Message<MessageEntity.Message> msg, MessageHeaders hdr,
      StompHeaderAccessor acc) {
    List<MessageEntity.Message> ret = List.of(
      MessageEntity.Message.builder()
        .type("my")
        .content("Hi! whatsup!?")
        .time("PM 01:10")
        .userId("tester")
        .unread(1)
      .build(),
      MessageEntity.Message.builder()
        .type("their")
        .content("Nothing special. How about you?")
        .avatar("/images/test.svg")
        .time("PM 01:10")
        .userId("tester")
        .unread(1)
      .build()
    );
    wsock.convertAndSend("/api/sub/chat-data", ret);
    return ret;
  }
}
