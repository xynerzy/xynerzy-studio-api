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

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.chatMessage.entity.ChatMessageEntity.ChatMessage;
import com.xynerzy.chatMessage.service.MessageService;
import com.xynerzy.main.entity.MainEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
public class MessageApiControl {

  static final String CONTROLLER_TAG1 = "Chatting Message API"; 
  
  private final MessageService messageService;

  @Operation(summary = "Send Chatting Messages", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_PUB + "/chat/{topic}")
  @MessageMapping("/chat/{topic}")
  public MainEntity.Result sendChatMessages(
    @PathVariable @DestinationVariable String topic,
    @Parameter(hidden = true) Message<ChatMessage> msg,
    @Parameter(hidden = true) MessageHeaders hdr,
    @Parameter(hidden = true) StompHeaderAccessor acc) throws Exception {
    log.debug("receive-chat:{} / {}", topic, msg);
    return messageService.sendChatMessages(msg, hdr, acc);
  }
  
  @Operation(summary = "Receive Chatting Messages", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = PTH_API + PTH_SUB + "/chat/{topic}")
  public List<ChatMessage> receiveMessages(
    @PathVariable String topic) throws Exception {
    return messageService.receiveMessages(topic, null);
  }
}
