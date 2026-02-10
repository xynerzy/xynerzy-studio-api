/**
 * @File        : ChatSessionEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.control;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;
import com.xynerzy.chatSession.service.ChatSessionService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api/chat-session")
public class ChatSessionApiControl {
  
  static final String CONTROLLER_TAG1 = "Chatting Session  API"; 

  private final ChatSessionService chatSessionService;
  
  @Operation(summary = "Chatting Session List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = "list")
  public List<ChatSession> chatSessionList() {
    return chatSessionService.chatSessionList();
  }
}
