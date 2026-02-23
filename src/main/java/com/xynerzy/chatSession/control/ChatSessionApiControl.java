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
import static com.xynerzy.commons.Constants.PTH_SUB;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;
import com.xynerzy.chatSession.service.ChatSessionService;
import com.xynerzy.main.entity.MainEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class ChatSessionApiControl {
  
  static final String CONTROLLER_TAG1 = "Chatting Session  API"; 

  private final ChatSessionService chatSessionService;

  @Operation(summary = "Send Chatting Session Message", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_PUB + "/session/{topic}")
  @MessageMapping("/session/{topic}")
  public MainEntity.Result sendSessionMessages(
    @PathVariable @DestinationVariable String topic,
    @RequestBody(required = false) ChatSession msg,
    @Parameter(hidden = true) Message<ChatSession> wsMsg) throws Exception {
    log.debug("chat-session:{} / {} / {}", topic, wsMsg, msg);
    Map<String, Object> attr = new LinkedHashMap<>();
    if (wsMsg != null) {
      StompHeaderAccessor wsAcc = StompHeaderAccessor.wrap(wsMsg);
      msg = wsMsg.getPayload();
      attr = wsAcc.getSessionAttributes();
    }
    return chatSessionService.sendSessionMessages(msg, attr);
  }
  
  @Operation(summary = "Receive Chatting Session Message", tags = { CONTROLLER_TAG1 })
  @GetMapping(path = PTH_API + PTH_SUB + "/session/{topic}")
  public List<ChatSession> receiveSessionMessages(
    @PathVariable String topic) throws Exception {
    return chatSessionService.receiveSessionMessages(topic, null);
  }
}
