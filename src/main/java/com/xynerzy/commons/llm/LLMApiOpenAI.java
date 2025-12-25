/**
 * @File        : LLMApiOpenAI.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of OpenAI API (ChatGPT, LM-Studio)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Slf4j @Component("openAIChatApi") @RequiredArgsConstructor
public class LLMApiOpenAI implements LLMApiBase {

  private final LLMProperties llmProperties;
  private final WebClient.Builder webClientBuilder;

  @Override public void streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LLMProperties.OpenAI openAIProps = llmProperties.getOpenai();

    /* Set the connection timeout to 5 seconds. */
    HttpClient httpClient = HttpClient.create()
      .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

    WebClient webClient = webClientBuilder
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .baseUrl(openAIProps.getBaseUrl())
      .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProps.getApiKey())
      .build();

    Map<String, Object> requestBody = Map.of(
      "model", openAIProps.getModel(),
      "messages", List.of(Map.of("role", "user", "content", request)),
      "stream", true
    );

    webClient.post()
      .uri("/chat/completions")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(requestBody)
      .retrieve()
      .bodyToFlux(String.class)
      .doOnNext(line -> {
        /**
         * The stream response comes in over multiple lines in the format "data: { ... }" 
         * When the response is complete, the message "data: [DONE]" is displayed.
         **/
        String data = "";
        if (line.startsWith("data:")) {
          data = line.substring(5).trim();

        } else if (line.startsWith("\u007b")) {
          /* {:007b ... }:007d */ 
          data = line;
        } else if ("[DONE]".equals(line)) {
          onNext.accept("\0\0");
          return;
        }
        try {
          JSONObject json = new JSONObject(data);
          String content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("delta")
            .optString("content");
          if (content != null && !content.isEmpty()) {
            onNext.accept(content);
          }
        } catch (Exception e) {
          onNext.accept("\0\0");
          log.error("Error parsing stream data: {}", data, e);
        }
      })
      .doOnComplete(onComplete)
      .doOnError(onError)
      .subscribe();
  }
}
