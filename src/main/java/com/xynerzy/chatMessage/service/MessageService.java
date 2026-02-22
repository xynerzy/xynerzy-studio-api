/**
 * @File        : MessageService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.service;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import com.xynerzy.chatMessage.entity.ChatMessageEntity;
import com.xynerzy.main.entity.MainEntity;

public interface MessageService {
  default MainEntity.Result sendChatMessages(Message<ChatMessageEntity.Message> msg, MessageHeaders hdr, StompHeaderAccessor acc) { return null; }
  default List<ChatMessageEntity.Message> receiveMessages(String topic, List<ChatMessageEntity.Message> list) { return list; }
}
