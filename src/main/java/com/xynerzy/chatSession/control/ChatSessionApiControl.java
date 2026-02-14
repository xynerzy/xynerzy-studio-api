/**
 * @File        : ChatSessionEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.control;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;
import com.xynerzy.chatSession.service.ChatSessionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
// @RequestMapping("/api/chat-session")
public class ChatSessionApiControl {
  
  static final String CONTROLLER_TAG1 = "Chatting Session  API"; 

  private final ChatSessionService chatSessionService;

  // @Operation(summary = "Chatting Session List", tags = { CONTROLLER_TAG1 })
  // @PostMapping(path = "list")
  // public List<ChatSession> chatSessionList() {
  //   return chatSessionService.chatSessionList();
  // }
  
  @Operation(summary = "Chatting Session List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = "/api/pub/chat-session/{sessionId}")
  @MessageMapping("/chat-session/{sessionId}")
  public void chatSessionList(
    @PathVariable @DestinationVariable String sessionId,
    @Nullable Message<ChatSession> msg,
    @Null MessageHeaders hdr,
    @Nullable StompHeaderAccessor acc) throws Exception {
    log.debug("chat-session:{} / {}", sessionId, msg);
    chatSessionService.chatSessionList(msg, hdr, acc);
  }
}
