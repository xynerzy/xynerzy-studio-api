/**
 * @File        : LLMApiGemini.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using API-KEY)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Component("geminiChatApi") @RequiredArgsConstructor
public class LLMApiGemini implements LLMApiBase {

  private final LLMProperties llmProperties;
  private final WebClient.Builder webClientBuilder;

  private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

  @Override
  public void streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LLMProperties.Gemini geminiProps = llmProperties.getGemini();
    WebClient webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build();
    GeminiRequest geminiRequest = createGeminiRequest(request);

    webClient.post()
      .uri(uriBuilder -> uriBuilder
        .path("/v1beta/models/gemini-1.5-flash:streamGenerateContent")
        .queryParam("key", geminiProps.getApiKey())
        .build())
      .bodyValue(geminiRequest)
      .retrieve()
      .bodyToFlux(GeminiResponse.class)
      .doOnNext(response -> {
        String text = response.extractText();
        if (!text.isEmpty()) {
          onNext.accept(text);
        }
      })
      .doOnComplete(onComplete)
      .doOnError(onError)
      .subscribe();
  }

  private GeminiRequest createGeminiRequest(String request) {
    GeminiRequest.Part part = new GeminiRequest.Part(request);
    GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
    return new GeminiRequest(List.of(content));
  }
    
  /* Request body Data model of Gemini API */
  @Data @AllArgsConstructor
  public static class GeminiRequest {
    private List<Content> contents;

    @Data @AllArgsConstructor
    public static class Content {
      private List<Part> parts;
    }

    @Data @AllArgsConstructor
    public static class Part {
      private String text;
    }
  }

  /* Response body Data model of Gemini API */
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class GeminiResponse {
    private List<Candidate> candidates;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
      private Content content;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
      private List<Part> parts;
      private String role;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
      private String text;
    }

    /* Extracting text from response */
    public String extractText() {
      if (candidates != null && !candidates.isEmpty()) {
        Content content = candidates.get(0).getContent();
        if (content != null && content.getParts() != null && !content.getParts().isEmpty()) {
          return content.getParts().get(0).getText();
        }
      }
      return "";
    }
  }
}