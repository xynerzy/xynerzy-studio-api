/**
 * @File        : LlmProperties.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM Settings from application.yml
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data @Configuration @ConfigurationProperties(prefix = "llm")
public class LLMProperties {

  private final OpenAI openai = new OpenAI();
  private final Gemini gemini = new Gemini();
  private final Ollama ollama = new Ollama();

  @Data public static class OpenAI {
    private String baseUrl;
    private String model;
    private String apiKey;
  }

  @Data public static class Gemini {
    private String baseUrl;
    private String model;
    private String apiKey;
    private String clientId;
    private String clientSecret;
    private String refreshToken;
  }

  @Data public static class Ollama {
    private String baseUrl;
    private String model;
  }
}
