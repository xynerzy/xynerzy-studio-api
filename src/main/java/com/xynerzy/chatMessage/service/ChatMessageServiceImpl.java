/**
 * @File        : MessageServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.service;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.xynerzy.chatMessage.entity.ChatMessageEntity.ChatMessage;
import com.xynerzy.main.entity.MainEntity;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
  
  private final SimpMessagingTemplate wsock;
  
  @PostConstruct public void init() {
    log.trace("INIT:{}", ChatMessageService.class);
  }

  @Override public MainEntity.Result sendChatMessages(ChatMessage msg, Map<String, Object> attr) {
    String sessionId = cast( attr.get("sessionId"), "");
    List<ChatMessage> ret = null;
    log.debug("MSG:{}", msg);
    if (msg.getType() == null && msg.getContent() == null) {
      ret = List.of(
        ChatMessage.builder()
          .type("my")
          .content("Hi! whatsup!?")
          .time("PM 01:10")
          .userId("tester")
          .unread(1)
        .build(),
        ChatMessage.builder()
          .type("their")
          .content("Nothing special. How about you?")
          .avatar("/images/test.svg")
          .time("PM 01:10")
          .userId("tester")
          .unread(1)
        .build()
      );
    } else {
      String content = msg.getContent();
      content = content.replaceAll("<br[ \\/]*>", "\n");
      ret = new ArrayList<>();
      ret.add(
        ChatMessage.builder()
          .type("my")
          .content(content)
          .time("PM 01:10")
          .userId("tester")
          .unread(0)
        .build()
      );
    }
    if (ret != null) { receiveMessages(concat("/api/sub/chat/", sessionId), ret); }
    return MainEntity.Result.builder().build();
  }

  @Override  public List<ChatMessage> receiveMessages(String topic, List<ChatMessage> list) {
    if (wsock != null) { wsock.convertAndSend(topic, list); }
    return list;
  }
}
