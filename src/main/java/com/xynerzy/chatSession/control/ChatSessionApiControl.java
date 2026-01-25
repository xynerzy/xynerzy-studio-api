/**
 * @File        : ChatSessionEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.control;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatSession.service.ChatSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api/chat-session")
public class ChatSessionApiControl {

  private final ChatSessionService chatSessionService;
  
  @PostMapping(path = "list")
  public Object chatSessionList() {
    return chatSessionService.chatSessionList();
  }
}
