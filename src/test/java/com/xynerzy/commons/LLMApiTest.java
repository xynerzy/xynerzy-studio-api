/**
 * @File        : LLMApiTest.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM Api Junit Testcase
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.xynerzy.commons.TestUtil.TestLevel;
import com.xynerzy.commons.llm.LLMApiBase;
import com.xynerzy.commons.llm.LLMApiGemini;
import com.xynerzy.commons.llm.LLMApiGeminiOAuth2;
import com.xynerzy.commons.llm.LLMApiOllama;
import com.xynerzy.commons.llm.LLMApiOpenAI;
import com.xynerzy.commons.llm.LLMProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMApiTest {
  @Test void testOpenAIApi() throws Exception {
    if (!TestUtil.isEnabled("testOpenAIApi", TestLevel.MANUAL)) { return; }
    LLMProperties props = new LLMProperties();
    /* "http://localhost:1234/v1" */
    props.setBaseUrl(System.getenv("OPENAI_API_BASE_URL"));
    /* "qwen/qwen3-coder-30b" */
    props.setModel(System.getenv("OPENAI_API_MODEL"));
    props.setApiKey(System.getenv("OPENAI_API_KEY"));

    WebClient.Builder wbldr = WebClient.builder();
    LLMApiBase api = new LLMApiOpenAI(props, wbldr);

    String request = "Hello? Who are you?";
    
    StringBuilder resp = new StringBuilder();
    LinkedBlockingQueue<Boolean> latch = new LinkedBlockingQueue<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    /* Act */
    log.info("Sending request to Open-AI...");
    api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        if ("\0\0".equals(chunk)) {
          latch.add(Boolean.TRUE);
        }
        resp.append(chunk);
      }, () -> {
        log.info("Stream completed.");
      }, err -> {
        log.error("Stream failed with an error", err);
        error.set(err);
        latch.add(Boolean.TRUE);
      }
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
    assertNull(error.get(), "Stream should complete without errors.");
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testGeminiApi() throws Exception {
    if (!TestUtil.isEnabled("testGeminiApi", TestLevel.MANUAL)) { return; }
    LLMProperties props = new LLMProperties();
    props.setApiKey(System.getenv("GEMINI_API_KEY"));
    /* "gemini-2.5-flash" */
    props.setModel(System.getenv("GEMINI_API_MODEL"));

    WebClient.Builder wbldr = WebClient.builder();
    LLMApiBase api = new LLMApiGemini(props, wbldr);

    String request = "Hello? Who are you?";

    StringBuilder resp = new StringBuilder();
    LinkedBlockingQueue<Boolean> latch = new LinkedBlockingQueue<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    /* Act */
    log.info("Sending request to Gemini API...");
    api.streamChat(
        request,
        chunk -> {
          log.debug("Received chunk: {}", chunk);
          resp.append(chunk);
        },
        () -> {
          log.info("Stream completed.");
          latch.add(Boolean.TRUE);
        },
        err -> {
          log.error("Stream failed with an error", err);
          error.set(err);
          latch.add(Boolean.TRUE);
        }
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertNull(error.get(), "Stream should complete without errors.");
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testGeminiApiOauth2() throws Exception {
    if (!TestUtil.isEnabled("testGeminiApiOauth2", TestLevel.MANUAL)) { return; }
    LLMProperties props = new LLMProperties();
    props.setClientId(System.getenv("GEMINI_API_CLIENT_ID"));
    props.setClientSecret(System.getenv("GEMINI_API_CLIENT_SECRET"));
    props.setRefreshToken(System.getenv("GEMINI_API_REFRESH_TOKEN"));
    /* "gemini-2.5-flash" */
    props.setModel(System.getenv("GEMINI_API_MODEL"));
    WebClient.Builder wbldr = WebClient.builder();
    LLMApiBase api = new LLMApiGeminiOAuth2(props, wbldr);

    String request = "Hello? Who are you?";

    StringBuilder resp = new StringBuilder();
    LinkedBlockingQueue<Boolean> latch = new LinkedBlockingQueue<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    /* Act */
    log.info("Sending request to Gemini API with OAuth2...");
    api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      }, () -> {
        log.info("Stream completed.");
        latch.add(Boolean.TRUE);
      }, err -> {
        log.error("Stream failed with an error", err);
        error.set(err);
        latch.add(Boolean.TRUE);
      }
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertNull(error.get(), "Stream should complete without errors.");
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testOllamaApi() throws InterruptedException {
    if (!TestUtil.isEnabled("testOllamaApi", TestLevel.MANUAL)) { return; }
    LLMProperties props = new LLMProperties();
    /* "http://localhost:11434" */
    props.setBaseUrl(System.getenv("OLLAMA_API_BASE_URL"));
    /* "gpt-oss:20b" */
    props.setModel(System.getenv("OLLAMA_API_MODEL"));

    WebClient.Builder wbldr = WebClient.builder();
    LLMApiOllama api = new LLMApiOllama(props, wbldr);

    String request = "Hello? Who are you?";

    StringBuilder resp = new StringBuilder();
    LinkedBlockingQueue<Boolean> latch = new LinkedBlockingQueue<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    /* Act */
    log.info("Sending request to Ollama API...");
    api.streamChat(
        request,
        chunk -> {
          log.debug("Received chunk: {}", chunk);
          resp.append(chunk);
        },
        () -> {
          log.info("Stream completed.");
          latch.add(Boolean.TRUE);
        },
        err -> {
          log.error("Stream failed with an error", err);
          error.set(err);
          latch.add(Boolean.TRUE);
        }
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertNull(error.get(), "Stream should complete without errors.");
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }
}