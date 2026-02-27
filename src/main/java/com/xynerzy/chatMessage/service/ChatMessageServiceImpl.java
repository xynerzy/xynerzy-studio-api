/**
 * @File        : MessageServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.service;

import static com.xynerzy.commons.DataUtil.valueOf;
import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

  private final Map<String, Object> chatCtx = new LinkedHashMap<>();
  
  @PostConstruct public void init() {
    log.trace("INIT:{}", ChatMessageService.class);
  }

  @Override public MainEntity.Result sendChatMessages(ChatMessage msg, Map<String, Object> attr) {
    String sessionId = cast(attr.get("sessionId"), "");
    Map<String, Object> cctx = valueOf(chatCtx.get(sessionId), new LinkedHashMap<>());
    chatCtx.put(sessionId, cctx);
    List<ChatMessage> ret = null;
    log.debug("SESS:{} / MSG:{}", sessionId, msg);
    log.debug("CCTX:{}", cctx);
    if (msg.getType() == null && msg.getContent() == null) {
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
        LLMApiBase api2 = new LLMApiOpenAI(props, wbldr);
        String sstr = cast(cctx.get("summary"), "");
        Map<String, String> request = Map.of(
          "user", content,
          "system", sstr != null ? concat("Previous summary: ", sstr) : "");
        StringBuilder sbuf = new StringBuilder();
        api.streamChat(
            request,
            chunk -> {
              sbuf.append(chunk);
              log.trace("Received chunk: {}", chunk);
              receiveMessages(concat("/api/sub/chat/", sessionId),
              List.of(
              ChatMessage.builder()
                .type("their")
                .messageId(messageId)
                .content(chunk)
                .avatar("/images/test.svg")
                .time(sdf.format(new Date()))
                .userId("tester")
                .unread(1)
              .build()));
            },
            () -> {
              if (sbuf.length() == 0) { return; }
              log.debug("REPLY:{} / {}", request.get("user"), sbuf);
              String reply = String.valueOf(sbuf);
              sbuf.setLength(0);
              try {
                api2.streamChat(
                  Map.of("user", String.format("Summarize this conversation in 1000 characters or less. \n%s\nA:%s\nB:%s", sstr, request.get("user"), reply)),
                  c -> {
                    // log.debug("C:{}", c);
                    sbuf.append(c);
                  },
                  () -> {
                    String summary = String.valueOf(sbuf);
                    log.info("SUMMARY[{}] : {}", summary.length(), summary);
                    cctx.put("summary", summary);
                  },
                  e -> { });
              } catch (Exception e) {
                log.info("E:", e);
              }
            },
            e -> log.error("Stream failed with an error", e)
        );
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
