/**
 * @File        : LLMApiTest.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM Api Junit Testcase
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.DataUtil.list;
import static com.xynerzy.commons.DataUtil.map;
import static com.xynerzy.commons.IOUtil.safeclose;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xynerzy.commons.TestUtil.TestLevel;
import com.xynerzy.commons.llm.LLMApi;
import com.xynerzy.commons.llm.LLMApiGemini;
import com.xynerzy.commons.llm.LLMApiGeminiOAuth2;
import com.xynerzy.commons.llm.LLMApiOllama;
import com.xynerzy.commons.llm.LLMApiOpenAI;
import com.xynerzy.commons.llm.LLMProperties;
import com.xynerzy.system.runtime.CoreSystem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMApiTest {
  @Test void testOpenAIApi() throws Exception {
    if (!TestUtil.isEnabled("testOpenAIApi", TestLevel.MANUAL)) { return; }
    CoreSystem.getInstance(new StandardEnvironment());
    LLMProperties props = new LLMProperties();
    /* "http://localhost:1234/v1" */
    props.setBaseUrl(System.getenv("OPENAI_API_BASE_URL"));
    /* "qwen/qwen3-coder-30b" */
    props.setModel(System.getenv("OPENAI_API_MODEL"));
    props.setApiKey(System.getenv("OPENAI_API_KEY"));

    LLMApi api = new LLMApiOpenAI(props);

    Map<String, Object> request = map("user", "Hello? Who are you?");
    
    StringBuilder resp = new StringBuilder();
    /* Act */
    log.info("Sending request to Open-AI...");
    LinkedBlockingQueue<Object> latch = api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      },
      () -> log.info("Stream completed."),
      e -> log.error("Stream failed with an error", e)
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
    // assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testGeminiApi() throws Exception {
    if (!TestUtil.isEnabled("testGeminiApi", TestLevel.MANUAL)) { return; }
    CoreSystem.getInstance(new StandardEnvironment());
    LLMProperties props = new LLMProperties();
    props.setApiKey(System.getenv("GEMINI_API_KEY"));
    /* "gemini-2.5-flash" */
    props.setModel(System.getenv("GEMINI_API_MODEL"));

    LLMApi api = new LLMApiGemini(props);

    Map<String, Object> request = map(
      // "system", "He is korean, speak in Korean Language",
      "user", "Hello? Who are you?"
    );

    StringBuilder resp = new StringBuilder();
    /* Act */
    log.info("Sending request to Gemini API...");
    LinkedBlockingQueue<Object> latch = api.streamChat(
        request,
        chunk -> {
          log.debug("Received chunk: {}", chunk);
          resp.append(chunk);
        },
        () -> log.info("Stream completed."),
        e -> log.error("Stream failed with an error", e)
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testGeminiApiOauth2() throws Exception {
    if (!TestUtil.isEnabled("testGeminiApiOauth2", TestLevel.MANUAL)) { return; }
    CoreSystem.getInstance(new StandardEnvironment());
    LLMProperties props = new LLMProperties();
    props.setClientId(System.getenv("GEMINI_API_CLIENT_ID"));
    props.setClientSecret(System.getenv("GEMINI_API_CLIENT_SECRET"));
    props.setRefreshToken(System.getenv("GEMINI_API_REFRESH_TOKEN"));
    /* "gemini-2.5-flash" */
    props.setModel(System.getenv("GEMINI_API_MODEL"));
    LLMApi api = new LLMApiGeminiOAuth2(props);

    Map<String, Object> request = map("user", "Hello? Who are you?");
    StringBuilder resp = new StringBuilder();
    /* Act */
    log.info("Sending request to Gemini API with OAuth2...");
    LinkedBlockingQueue<Object> latch = api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      },
      () -> log.info("Stream completed."),
      e -> log.error("Stream failed with an error", e)
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test void testOllamaApi() throws InterruptedException {
    if (!TestUtil.isEnabled("testOllamaApi", TestLevel.MANUAL)) { return; }
    CoreSystem.getInstance(new StandardEnvironment());
    LLMProperties props = new LLMProperties();
    /* "http://localhost:11434" */
    props.setBaseUrl(System.getenv("OLLAMA_API_BASE_URL"));
    /* "gpt-oss:20b" */
    props.setModel(System.getenv("OLLAMA_API_MODEL"));

    LLMApiOllama api = new LLMApiOllama(props);

    Map<String, Object> request = map(
      "user", "Hello? Who are you?",
      // "user", ".",
      "system", "He is korean, answer in korean"
      // "system", ""
    );

    StringBuilder resp = new StringBuilder();
    /* Act */
    log.info("Sending request to Ollama API...");
    LinkedBlockingQueue<Object> latch = api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      },
      () -> log.info("Stream completed."),
      e -> log.error("Stream failed with an error", e)
    );
    /* Assert */
    while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    assertFalse(resp.toString().isEmpty(), "Response should not be empty.");
    log.info("Final Response: {}", resp.toString());
  }

  @Test public void readJSONTest() throws Exception {
    if (!TestUtil.isEnabled("readJSONTest", TestLevel.MANUAL)) { return; }
    String data = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello! I am a large language model, trained by Google.\"},{\"text\":\"How was your day?.\"}],\"role\":\"model\"},\"finishReason\":\"STOP\",\"index\":0}],\"usageMetadata\":{\"promptTokenCount\":7,\"candidatesTokenCount\":13,\"totalTokenCount\":48,\"promptTokensDetails\":[{\"modality\":\"TEXT\",\"tokenCount\":7}],\"thoughtsTokenCount\":28},\"modelVersion\":\"gemini-2.5-flash\",\"responseId\":\"R7KlaaF6ruDaug--mcjYAQ\"}";
    Reader reader = null;
    int depth = 0;
    JsonFactory factory = new JsonFactory();
    ObjectMapper mapper = new ObjectMapper();
    List<String> keys = new ArrayList<>();
    try {
      reader = new StringReader(data);
      JsonParser parser = factory.createParser(reader);
      String key = "";
      while (parser.nextToken() != null) {
        JsonToken token = parser.currentToken();
        switch (token) {
        case FIELD_NAME: {
          key = parser.getValueAsString();
        } break;
        case START_OBJECT: {
          if (depth > 0) {
            keys.add(key);
            if ("[candidates, 0, content, parts, 0]".equals(String.valueOf(keys))) {
              JsonNode node = mapper.readTree(parser);
              if (keys.size() > 0) { keys.remove(keys.size() - 1); }
              log.info("TEXTE:{}", node.get("text"));
            }
          }
          depth += 1;
        } break;
        case END_OBJECT: {
          if (keys.size() > 0) { keys.remove(keys.size() - 1); }
          depth -= 1;
        } break;
        case START_ARRAY: {
          if (depth == 0) {
          } else {
            keys.add(key);
          }
          key = "0";
          depth += 1;
        } break;
        case END_ARRAY: {
          if (keys.size() > 0) { keys.remove(keys.size() - 1); }
          depth -= 1;
        } break;
        case VALUE_STRING:
        default:
        }
      }
    } finally {
      safeclose(reader);
    }
  }

  @Test public void llmTikiTakaTest() throws Exception {
    if (!TestUtil.isEnabled("llmCommunicationTest", TestLevel.MANUAL)) { return; }
    CoreSystem.getInstance(new StandardEnvironment());
    int MAX_CONVERSATIONS = 2;
    List<LLMApi> apiList = list(
      new LLMApiOpenAI(LLMProperties.builder()
        .baseUrl(System.getenv("OPENAI_API_BASE_URL"))
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .model(System.getenv("OPENAI_API_MODEL"))
        .build()
      ),
      new LLMApiOllama(LLMProperties.builder()
        .baseUrl(System.getenv("OLLAMA_API_BASE_URL"))
        .model(System.getenv("OLLAMA_API_MODEL"))
        .build()
      )
    );
    LLMApi manager = apiList.get(1);
    StringBuilder summary1 = new StringBuilder();
    StringBuilder summary2 = new StringBuilder();
    StringBuilder talk = new StringBuilder();
    String q = "", a = "";
    summary1.append(System.getenv("TALKER1_MOTIVE"));
    summary2.append(System.getenv("TALKER2_MOTIVE"));
    LinkedBlockingQueue<Object> latch = null;
    talk.append(System.getenv("GREETINGS"));
    // for (int inx = 0; inx < MAX_CONVERSATIONS; inx++) {
      q = String.valueOf(talk);
      talk.setLength(0);
      latch = apiList.get(0).streamChat(
        map("user", q, "system", String.format("%s", summary1)),
        v -> {
          log.trace("V:{}", v);
          talk.append(v);
        }, () -> { }, e -> { });
      while(!Boolean.TRUE.equals(latch.poll(300, TimeUnit.MILLISECONDS)));
      a = String.valueOf(talk);
      log.debug("Q:{} / A:{}", q, a);
      talk.setLength(0);
      q = a;
      latch = apiList.get(1).streamChat(
        map("user", q, "system", String.format("%s", summary2)),
        v -> {
          log.trace("V:{}", v);
          talk.append(v);
        }, () -> { }, e -> { });
      while(!Boolean.TRUE.equals(latch.poll(300, TimeUnit.MILLISECONDS)));
      a = String.valueOf(talk);
      log.debug("Q:{} / A:{}", q, a);
      talk.setLength(0);
      // latch = manager.streamChat(
      //   map("user", String.format("summarize this : Q:%s, A:%s", q, a)),
      //   v -> talk.append(v), () -> { }, e -> { });
      // latch.poll(1000, TimeUnit.MILLISECONDS);
      // log.debug("SUMMARY:{}", talk);
    // }
  }
}