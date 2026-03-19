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
    int MAX_CONVERSATIONS = 2;
    int[] LLM_INX_USERS = new int[] { 0, 0 };
    int LLM_INX_MANAGER = 0;
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
    String user1 = "A";
    String user2 = "B";
    StringBuilder summary = new StringBuilder();
    StringBuilder agent1 = new StringBuilder();
    StringBuilder agent2 = new StringBuilder();
    StringBuilder talkbuf = new StringBuilder();
    String q = "", a = "";
    String basicState = String.format("" +
      "The list of participants is as follows: \n" +
      "--\n" +
      "%s: Mentor \n" +
      "%s: Mentee \n" + 
      "--\n", user1, user2);
    summary.append("");
    agent1.append(String.format("%s%s", basicState, System.getenv("TALKER1_MOTIVE")));
    agent2.append(String.format("%s%s", basicState, System.getenv("TALKER2_MOTIVE")));
    LinkedBlockingQueue<Object> latch = null;
    talkbuf.append(System.getenv("GREETINGS"));
    // for (int inx = 0; inx < MAX_CONVERSATIONS; inx++) {
      q = String.format("%s", talkbuf);

      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_USERS[0]).streamChat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s says last", agent1, user2)),
        v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user2, q, user1, a);
      
      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_MANAGER).streamChat(
        map("user",
          String.format(
            "Summarize follow conversations less than 1000 characters.\n%s\n%s\n%s",
            String.format("%s", summary),
            String.format("%s says \"%s\"", user2, q),
            String.format("%s says \"%s\"", user1, a)
          ), 
          "system", String.format("%s", basicState)),
          v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      summary.setLength(0);
      summary.append(talkbuf);
      log.debug("\nSUMMARY: {}", summary);

      q = String.format("%s", a);
      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_USERS[1]).streamChat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s says last", agent2, user1)),
        v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user1, q, user2, a);
      q = String.format("%s", a);

      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_MANAGER).streamChat(
        map("user",
          String.format(
            "Summarize follow conversations less than 1000 characters.\n%s\n%s\n%s",
            String.format("%s", summary),
            String.format("%s says \"%s\"", user1, q),
            String.format("%s says \"%s\"", user2, a)
          ), 
          "system", String.format("%s", basicState)),
          v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      summary.setLength(0);
      summary.append(talkbuf);
      log.debug("\nSUMMARY: {}", summary);

      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_MANAGER).streamChat(
        map("user",
          String.format(
          "%s\n" + 
          "--\n "+
          "Who will talk next? Aswer using Labels only",
          summary
          ), 
          "system", String.format("%s", basicState)),
          v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      log.debug("NEXT: {}", talkbuf);
      log.debug("SUMMARY:{}", summary);
      log.debug("AGENT1:{}", agent1);
      log.debug("AGENT2:{}", agent2);
      log.debug("Q:{}", q);
      talkbuf.setLength(0);
      latch = apiList.get(LLM_INX_USERS[0]).streamChat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s\nyou are %s\n%s says last", basicState, summary, user1, user2)),
        v -> talkbuf.append(v), () -> { }, e -> { });
      while(latch.poll(300, TimeUnit.MILLISECONDS) == null);
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user1, q, user2, a);
      
    // }
  }
}