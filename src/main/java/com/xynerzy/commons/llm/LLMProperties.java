/**
 * @File        : LlmProperties.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM Settings from application.yml
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class LLMProperties {
  private String baseUrl;
  private String model;
  private String apiKey;
  private String uriTemplate;
  private String clientId;
  private String clientSecret;
  private String refreshToken;
}
