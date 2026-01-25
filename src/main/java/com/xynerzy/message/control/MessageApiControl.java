/**
 * @File        : MessageApiControl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.control;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.message.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api/message")
public class MessageApiControl {
  
  private final MessageService messageService;
  
  @PostMapping(path = "list")
  public Object messageList() {
    return messageService.messageList();
  }
}
