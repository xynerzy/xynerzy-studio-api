/**
 * @File        : LLMApiGeminiOAuth2.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using O-Auth2)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.xynerzy.commons.llm.LLMApiGemini.GeminiRequest;
import com.xynerzy.commons.llm.LLMApiGemini.GeminiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiGeminiOAuth2 implements LLMApiBase {

  private final LLMProperties props;
  private final WebClient.Builder webClientBuilder;

  @Override
  public LinkedBlockingQueue<Object> streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    return streamChatWithOauth2(request, onNext, onComplete, onError);
  }

  private LinkedBlockingQueue<Object> streamChatWithOauth2(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    try {
      /* 1. Issue an access token using a refresh token */
      String baseUrl = props.getBaseUrl();
      String template = props.getUriTemplate();
      if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
      if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }
      String accessToken = new GoogleRefreshTokenRequest(
        new NetHttpTransport(),
        new GsonFactory(),
        props.getRefreshToken(),
        props.getClientId(),
        props.getClientSecret())
        .execute()
        .getAccessToken();

      /* 2. Call the API using the issued access token. */
      WebClient webClient = webClientBuilder.baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .build();

      GeminiRequest geminiRequest = LLMApiGemini.createGeminiRequest(request);

      final String TEMPLATE = template;
      webClient.post()
        .uri(TEMPLATE.replaceAll("\\$\\{MODEL\\}", props.getModel()))
        .bodyValue(geminiRequest)
        .retrieve()
        .bodyToFlux(GeminiResponse.class)
        .doOnNext(response -> onNext.accept(response.extractText()))
        .doOnComplete(() -> {
          onComplete.run();
          ret.add(Boolean.TRUE);
        })
        .doOnError(e -> {
          onError.accept(e);
          ret.add(e);
        })
        .subscribe();

    } catch (IOException e) {
      log.error("Failed to refresh access token", e);
      onError.accept(e);
      ret.add(e);
    }
    return ret;
  }
}
