/**
 * @File        : ChatSessionService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.service;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;

public interface ChatSessionService {
  default List<ChatSession> chatSessionList() { return null; }
  default void chatSessionList(Message<ChatSession> msg, MessageHeaders hdr, StompHeaderAccessor acc) { }
}
