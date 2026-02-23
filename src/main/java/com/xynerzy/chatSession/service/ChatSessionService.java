/**
 * @File        : ChatSessionService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.service;

import java.util.List;
import java.util.Map;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;
import com.xynerzy.main.entity.MainEntity;

public interface ChatSessionService {
  default MainEntity.Result sendSessionMessages(ChatSession msg, Map<String, Object> attr) { return null; }
  default List<ChatSession> receiveSessionMessages(String topic, List<ChatSession> list) { return list; }
}
