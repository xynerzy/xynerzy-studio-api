/**
 * @File        : MessageApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.control;

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

import com.xynerzy.message.entity.MessageEntity;
import com.xynerzy.message.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class MessageApiControl {

  static final String CONTROLLER_TAG1 = "Chatting Message API"; 
  
  private final MessageService messageService;

  @Operation(summary = "Chatting Message List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_PUB + "/chat/{sessionId}")
  @MessageMapping("/chat/{sessionId}")
  public List<MessageEntity.Message> messageList(
    @PathVariable @DestinationVariable String sessionId,
    @Parameter(hidden = true) Message<MessageEntity.Message> msg,
    @Parameter(hidden = true) MessageHeaders hdr,
    @Parameter(hidden = true) StompHeaderAccessor acc) throws Exception {
    log.debug("receive-chat:{} / {}", sessionId, msg);
    return messageService.messageList(msg, hdr, acc);
  }
}
