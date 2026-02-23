/**
 * @File        : MessageApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.control;

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
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatMessage.entity.ChatMessageEntity.ChatMessage;
import com.xynerzy.chatMessage.service.ChatMessageService;
import com.xynerzy.main.entity.MainEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class ChatMessageApiControl {

  static final String CONTROLLER_TAG1 = "Chatting Message API"; 
  
  private final ChatMessageService messageService;

  @Operation(summary = "Send Chatting Messages", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_PUB + "/chat/{topic}")
  @MessageMapping("/chat/{topic}")
  public MainEntity.Result sendChatMessages(
    @PathVariable @DestinationVariable String topic,
    @RequestBody(required = false) ChatMessage msg,
    @Parameter(hidden = true) Message<ChatMessage> wsMsg) throws Exception {
    log.debug("receive-chat:{} / {} / {}", topic, wsMsg, msg);
    Map<String, Object> attr = new LinkedHashMap<>();
    if (wsMsg != null) {
      StompHeaderAccessor wsAcc = StompHeaderAccessor.wrap(wsMsg);
      msg = wsMsg.getPayload();
      attr = wsAcc.getSessionAttributes();
    }
    return messageService.sendChatMessages(msg, attr);
  }
  
  @Operation(summary = "Receive Chatting Messages", tags = { CONTROLLER_TAG1 })
  @GetMapping(path = PTH_API + PTH_SUB + "/chat/{topic}")
  public List<ChatMessage> receiveMessages(
    @PathVariable String topic) throws Exception {
    return messageService.receiveMessages(topic, null);
  }
}
