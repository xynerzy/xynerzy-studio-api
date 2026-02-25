/**
 * @File        : LLMApiGemini.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using API-KEY)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  public LinkedBlockingQueue<Object> streamChat(Map<String, String> request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    String baseUrl = props.getBaseUrl();
    String template = props.getUriTemplate();
    if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
    if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }

    WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
    GeminiRequest geminiRequest = createGeminiRequest(request);
    // try {
    //   log.debug("GEMINI-REQUEST:{}", new ObjectMapper().writeValueAsString(geminiRequest));
    // } catch (Exception ignore) { }
    // GEMINI-REQUEST:{"contents":[{"role":"user","parts":[{"text":"Hello! Who are you?"}]}]}
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
        // log.debug("NEXT:{}", response);
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

  public static GeminiRequest createGeminiRequest(Map<String, String> request) {
    List<GeminiRequest.Content> contents = new ArrayList<>();
    for (String key : request.keySet()) {
      GeminiRequest.Part part = new GeminiRequest.Part(request.get(key));
      GeminiRequest.Content content = new GeminiRequest.Content(key, List.of(part));
      contents.add(content);
    }
    return new GeminiRequest(contents);
  }
    
  /* Request body Data model of Gemini API */
  @Data @AllArgsConstructor
  public static class GeminiRequest {
    private List<Content> contents;

    @Data @AllArgsConstructor
    public static class Content {
      private String role;
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