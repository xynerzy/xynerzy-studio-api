/**
 * @File        : MessageService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.service;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import com.xynerzy.main.entity.MainEntity;
import com.xynerzy.message.entity.MessageEntity;

public interface MessageService {
  default MainEntity.Result sendChatMessages(Message<MessageEntity.Message> msg, MessageHeaders hdr, StompHeaderAccessor acc) { return null; }
  default List<MessageEntity.Message> receiveMessages(String topic, List<MessageEntity.Message> list) { return list; }
}
