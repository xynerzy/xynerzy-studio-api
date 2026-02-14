/**
 * @File        : MessageService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.service;

import java.util.List;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import com.xynerzy.message.entity.MessageEntity;

public interface MessageService {
  default List<MessageEntity.Message> messageList() { return null; }

  void messageList(org.springframework.messaging.Message<MessageEntity.Message> msg, MessageHeaders hdr, StompHeaderAccessor acc);;
}
