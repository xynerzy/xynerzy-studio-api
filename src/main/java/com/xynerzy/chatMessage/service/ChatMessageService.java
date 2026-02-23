/**
 * @File        : MessageService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.service;

import java.util.List;
import java.util.Map;

import com.xynerzy.chatMessage.entity.ChatMessageEntity.ChatMessage;
import com.xynerzy.main.entity.MainEntity;

public interface ChatMessageService {
  default MainEntity.Result sendChatMessages(ChatMessage msg, Map<String, Object> attr) { return null; }
  default List<ChatMessage> receiveMessages(String topic, List<ChatMessage> list) { return list; }
}
