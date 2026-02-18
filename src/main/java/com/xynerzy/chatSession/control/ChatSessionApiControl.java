/**
 * @File        : ChatSessionEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.control;

import static com.xynerzy.commons.Constants.PTH_API;
import static com.xynerzy.commons.Constants.PTH_PUB;

import java.util.List;

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
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class ChatSessionApiControl {
  
  static final String CONTROLLER_TAG1 = "Chatting Session  API"; 

  private final ChatSessionService chatSessionService;

  @Operation(summary = "Chatting Session List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_PUB + "/session/{userId}")
  @MessageMapping("/session/{userId}")
  public List<ChatSession> chatSessionList(
    @PathVariable @DestinationVariable String userId,
    @Parameter(hidden = true) Message<ChatSession> msg,
    @Parameter(hidden = true) MessageHeaders hdr,
    @Parameter(hidden = true) StompHeaderAccessor acc) throws Exception {
    log.debug("chat-session:{} / {}", userId, msg);
    return chatSessionService.chatSessionList(msg, hdr, acc);
  }
}
