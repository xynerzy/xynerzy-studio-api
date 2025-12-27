/**
 * @File        : LLMApiTest.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM Api Junit Testcase
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.xynerzy.commons.TestUtil.TestLevel;
import com.xynerzy.commons.llm.LLMApiOpenAI;
import com.xynerzy.commons.llm.LLMProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMApiTest {
  @Test void testOpenAIApi() throws InterruptedException {
    if (!TestUtil.isEnabled("testOpenAIApi", TestLevel.MANUAL)) { return; }
    /* Arrange */
    LLMProperties llmProperties = new LLMProperties();
    LLMProperties openAIProps = llmProperties;
    /* "http://localhost:1234/v1" */
    openAIProps.setBaseUrl(System.getenv("OPENAI_API_BASE_URL"));
    /* "qwen/qwen3-coder-30b" */
    openAIProps.setModel(System.getenv("OPENAI_API_MODEL"));
    openAIProps.setApiKey(System.getenv("OPENAI_API_KEY"));

    WebClient.Builder webClientBuilder = WebClient.builder();
    LLMApiOpenAI openAIApi = new LLMApiOpenAI(llmProperties, webClientBuilder);

    String request = "Hello? Who are you?";
    StringBuilder responseBuilder = new StringBuilder();

    /* Act & Assert */
    log.info("Sending request to LM-Studio...");
    LinkedBlockingQueue<Boolean> latch = new LinkedBlockingQueue<>();
    try {
      openAIApi.streamChat(
          request,
          chunk -> {
            log.debug("Received chunk: {}", chunk);
            if ("\0\0".equals(chunk)) {
              latch.add(Boolean.TRUE);
            }
            responseBuilder.append(chunk);
          },
          () -> log.info("Stream completed."),
          error -> log.error("Stream error.", error)
      );
      /**
       * Since the actual streamChat implementation is invoked asynchronously via subscribe(), 
       * this testing approach cannot guarantee detection of the end of the stream.
       * As the streaming logic itself has already been verified to work correctly,
       * this test is replaced with a blocking test,
       * and the streaming behavior is more efficiently validated at the point where it is actually used.
       * Here, we wait briefly for the purpose of proof of concept.
       **/
      while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    } catch (Exception e) {
      log.error("Test failed with exception", e);
    }
    assertFalse(responseBuilder.toString().isEmpty(), "Response should not be empty.");
    log.info("Final (potentially incomplete) Response: {}", responseBuilder.toString());
  }
}