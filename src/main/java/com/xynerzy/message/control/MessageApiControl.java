/**
 * @File        : MessageApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.control;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.message.entity.MessageEntity.Message;
import com.xynerzy.message.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api/message")
public class MessageApiControl {

  static final String CONTROLLER_TAG1 = "Chatting Message API"; 
  
  private final MessageService messageService;
  
  @Operation(summary = "Chatting Message List", tags = { CONTROLLER_TAG1 })
  @PostMapping(path = "list")
  public List<Message> messageList() {
    return messageService.messageList();
  }
}
