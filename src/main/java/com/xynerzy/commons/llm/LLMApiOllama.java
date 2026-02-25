/**
 * @File        : LLMApiOllama.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Ollama API
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiOllama implements LLMApiBase {

  private final LLMProperties props;
  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LLMApiOllama(LLMProperties llmProperties, WebClient.Builder webClientBuilder) {
    this.props = llmProperties;
    this.webClient = webClientBuilder.baseUrl(llmProperties.getBaseUrl()).build();
  }

  @Override
  public LinkedBlockingQueue<Object> streamChat(Map<String, String> request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    OllamaRequest ollamaRequest = new OllamaRequest(props.getModel(), request.get("user"), request.get("system"), true);

    webClient.post()
      .uri("/api/generate")
      .bodyValue(ollamaRequest)
      .retrieve()
      .bodyToFlux(String.class)
      .map(line -> {
        try {
          return objectMapper.readValue(line, OllamaResponse.class);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse Ollama response: " + line, e);
        }
      })
      .takeUntil(OllamaResponse::isDone)
      .subscribe(
        response -> {
          if (response.getResponse() != null) {
            onNext.accept(response.getResponse());
          }
        },
        e -> {
          onError.accept(e);
          ret.add(e);
        },
        () -> {
          onComplete.run();
          ret.add(Boolean.TRUE);
        }
      );
    return ret;
  }

  @Data @AllArgsConstructor
  private static class OllamaRequest {
    private String model;
    private String prompt;
    private String system;
    private boolean stream;
  }

  @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
  private static class OllamaResponse {
    private String model;
    private String created_at;
    private String response;
    private boolean done;
    private long total_duration;
    private long load_duration;
    private int prompt_eval_count;
    private long prompt_eval_duration;
    private int eval_count;
    private long eval_duration;
    // private int[] context;
  }
}