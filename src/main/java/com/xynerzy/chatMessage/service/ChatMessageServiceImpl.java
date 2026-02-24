/**
 * @File        : MessageServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.service;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.xynerzy.chatMessage.entity.ChatMessageEntity.ChatMessage;
import com.xynerzy.commons.llm.LLMApiBase;
import com.xynerzy.commons.llm.LLMApiOpenAI;
import com.xynerzy.commons.llm.LLMProperties;
import com.xynerzy.main.entity.MainEntity;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
  
  private final SimpMessagingTemplate wsock;
  
  @PostConstruct public void init() {
    log.trace("INIT:{}", ChatMessageService.class);
  }

  @Override public MainEntity.Result sendChatMessages(ChatMessage msg, Map<String, Object> attr) {
    String sessionId = cast(attr.get("sessionId"), "");
    List<ChatMessage> ret = null;
    log.debug("MSG:{}", msg);
    if (msg.getType() == null && msg.getContent() == null) {
      // ret = List.of(
      //   ChatMessage.builder()
      //     .type("my")
      //     .content("Hi! whatsup!?")
      //     .time("PM 01:10")
      //     .userId("tester")
      //     .unread(1)
      //   .build(),
      //   ChatMessage.builder()
      //     .type("their")
      //     .content("Nothing special. How about you?")
      //     .avatar("/images/test.svg")
      //     .time("PM 01:10")
      //     .userId("tester")
      //     .unread(1)
      //   .build()
      // );
    } else {
      String content = msg.getContent();
      content = content.replaceAll("<br[ \t\r\n\\/]*>", "\n").trim();
      ret = new ArrayList<>();
      SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm");
      ret.add(
        ChatMessage.builder()
          .type("my")
          .messageId(UUID.randomUUID().toString())
          .content(content)
          .time(sdf.format(new Date()))
          .userId(sessionId)
          .unread(1)
        .build()
      );
      {
        String messageId = UUID.randomUUID().toString();
        LLMProperties props = new LLMProperties();
        // props.setApiKey(System.getenv("GEMINI_API_KEY"));
        // props.setModel(System.getenv("GEMINI_API_MODEL"));
        props.setBaseUrl(System.getenv("OPENAI_API_BASE_URL"));
        props.setModel(System.getenv("OPENAI_API_MODEL"));
        props.setApiKey(System.getenv("OPENAI_API_KEY"));

        WebClient.Builder wbldr = WebClient.builder();
        // LLMApiBase api = new LLMApiGemini(props, wbldr);
        LLMApiBase api = new LLMApiOpenAI(props, wbldr);

        String request = content;

        // StringBuilder resp = new StringBuilder();
        // log.info("Sending request to Gemini API...");
        LinkedBlockingQueue<Object> latch = api.streamChat(
            request,
            chunk -> {
              log.debug("Received chunk: {}", chunk);
              // resp.append(chunk);
              receiveMessages(concat("/api/sub/chat/", sessionId),
              List.of(
              ChatMessage.builder()
                .messageId(messageId)
                .type("their")
                .content(chunk)
              .build()));
            },
            () -> log.info("Stream completed."),
            e -> log.error("Stream failed with an error", e)
        );
        /* Assert */
        // while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
        // assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
        // log.info("Final Response: {}", resp.toString());
      }
    }
    if (ret != null) { receiveMessages(concat("/api/sub/chat/", sessionId), ret); }
    return MainEntity.Result.builder().build();
  }

  @Override  public List<ChatMessage> receiveMessages(String topic, List<ChatMessage> list) {
    if (wsock != null) { wsock.convertAndSend(topic, list); }
    return list;
  }
}
