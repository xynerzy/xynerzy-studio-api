/**
 * @File        : ChatSessionServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of ChatSession Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.service;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import com.xynerzy.chatSession.entity.ChatSessionEntity.ChatSession;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {
  
  private final SimpMessagingTemplate wsock;

  @PostConstruct public void init() {
    log.trace("INIT:{}", ChatSessionService.class);
  }

  @Override public List<ChatSession> chatSessionList(Message<ChatSession> msg, MessageHeaders hdr, StompHeaderAccessor acc) {
    Map<String, Object> attr = acc.getSessionAttributes();
    String userId = cast(attr.get("userId"), "");
    List<ChatSession> ret = List.of(
      ChatSession.builder()
        .name("tester")
        .intro("hello!")
        .members(List.of("/images/test.svg"))
        .active(true)
        .online(true)
        .updated("PM 01:10")
        .unread(0)
      .build()
    );
    // log.debug("RECEIVE-CHAT:{}", msg.getPayload());
    // log.debug("MESSAGE:{} / {} / {}", acc.getSessionId(), msg, hdr);
    // String roomId = cast(acc.getFirstNativeHeader("x-chatsession-id"), "");
    // String userId = null;
    // Map<String, Object> atr = acc.getSessionAttributes();
    // log.debug("SESSION:{} / {}", userId, atr);
    if (wsock != null) { wsock.convertAndSend(concat("/api/sub/session/", userId), ret); }
    // if (atr != null) {
    //   userId = cast(atr.get("userId"), "");
    // }
    // Date cdate = new Date();
    // ChatEntity dto = msg.getPayload();
    // if (roomId != null) {
    //   String topic = cat(CHAT, ".", roomId);
    //   /** 채팅방이 없는경우 생성한다. */
    //   if (!chatroomRepo.existsByRoomId(roomId)) {
    //     chatroomRepo.save(ChatRoomEntity.builder()
    //       .roomId(roomId)
    //       .ctime(cdate)
    //       .utime(cdate)
    //       .build());
    //   }
    //   /** TODO: 채팅 메시지 act 를 구분할것 (입장, 퇴장, 키보드입력, 메시지전송 등) */
    //   if (!"".equals(dto.getMessage())) {
    //     dto.setSid(acc.getSessionId());
    //     dto.setUserId(userId);
    //     dto.setTopic(topic);
    //     dto.setPartition(0);
    //     dto.setChatId(ChatId.builder()
    //       .roomId(roomId)
    //       .build());
    //     dto.setKey(CHAT);
    //     dto.setSvr(core.getServerId());
    //     kafkaService.sendChatMessage(dto);
    //     log.debug("KAFKA-SENDED:{}", dto);
    //   }
    // }
    return ret;
  }
}
