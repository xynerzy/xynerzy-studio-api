/**
 * @File        : LLMApiGemini.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using API-KEY)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiGemini implements LLMApiBase {

  private final LLMProperties props;
  private final WebClient.Builder webClientBuilder;

  @Override
  public LinkedBlockingQueue<Object> streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    String baseUrl = props.getBaseUrl();
    String template = props.getUriTemplate();
    if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
    if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }

    WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
    GeminiRequest geminiRequest = createGeminiRequest(request);

    final String TEMPLATE = template;
    webClient.post()
      .uri(uriBuilder -> uriBuilder
        .path(TEMPLATE.replaceAll("\\$\\{MODEL\\}", props.getModel()))
        .queryParam("key", props.getApiKey())
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
      .doOnComplete(() -> {
        onComplete.run();
        ret.add(Boolean.TRUE);
      })
      .doOnError(e -> {
        onError.accept(e);
        ret.add(e);
      })
      .subscribe();
    return ret;
  }

  public static GeminiRequest createGeminiRequest(String request) {
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