/**
 * @File        : MessageApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.control;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.message.entity.MessageEntity;
import com.xynerzy.message.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class MessageApiControl {

  static final String CONTROLLER_TAG1 = "Chatting Message API"; 
  
  private final MessageService messageService;

  @Operation(summary = "Chatting Message List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = "/api/pub/chat/{sessionId}")
  @MessageMapping("/chat/{sessionId}")
  public void messageList(
    @PathVariable @DestinationVariable String sessionId,
    @Nullable Message<MessageEntity.Message> msg,
    @Null MessageHeaders hdr,
    @Nullable StompHeaderAccessor acc) throws Exception {
    log.debug("receive-chat:{} / {}", sessionId, msg);
    messageService.messageList(msg, hdr, acc);
  }
}
