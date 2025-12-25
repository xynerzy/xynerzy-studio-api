/**
 * @File        : LLMApiGeminiOAuth2.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using O-Auth2)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.xynerzy.commons.llm.LLMApiGemini.GeminiRequest;
import com.xynerzy.commons.llm.LLMApiGemini.GeminiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Component("geminiChatApiOAuth2") @RequiredArgsConstructor
public class LLMApiGeminiOAuth2 implements LLMApiBase {

  private final LLMProperties llmProperties;
  private final WebClient.Builder webClientBuilder;

  private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

  @Override
  public void streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    streamChatWithOauth2(request, onNext, onComplete, onError);
  }

  private void streamChatWithOauth2(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    try {
      /* 1. Issue an access token using a refresh token */
      LLMProperties.Gemini oauth2Props = llmProperties.getGemini();
      String accessToken = new GoogleRefreshTokenRequest(
        new NetHttpTransport(),
        new GsonFactory(),
        oauth2Props.getRefreshToken(),
        oauth2Props.getClientId(),
        oauth2Props.getClientSecret())
        .execute()
        .getAccessToken();

      /* 2. Call the API using the issued access token. */
      WebClient webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .build();

      GeminiRequest geminiRequest = createGeminiRequest(request);

      webClient.post()
        .uri("/v1beta/models/gemini-1.5-flash:streamGenerateContent")
        .bodyValue(geminiRequest)
        .retrieve()
        .bodyToFlux(GeminiResponse.class)
        .doOnNext(response -> onNext.accept(response.extractText()))
        .doOnComplete(onComplete)
        .doOnError(onError)
        .subscribe();

    } catch (IOException e) {
      log.error("Failed to refresh access token", e);
      onError.accept(e);
    }
  }

  private GeminiRequest createGeminiRequest(String request) {
    GeminiRequest.Part part = new GeminiRequest.Part(request);
    GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
    return new GeminiRequest(List.of(content));
  }
}
