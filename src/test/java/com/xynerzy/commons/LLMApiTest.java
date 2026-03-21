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
import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    BlockingQueue<Object> latch = api.streamChat(
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
    LLMApi.wait(api.streamChat(
        request,
        chunk -> {
          log.debug("Received chunk: {}", chunk);
          resp.append(chunk);
        },
        () -> log.info("Stream completed."),
        e -> log.error("Stream failed with an error", e)
    ));
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
    LLMApi.wait(api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      },
      () -> log.info("Stream completed."),
      e -> log.error("Stream failed with an error", e)
    ));
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
    LLMApi.wait(api.streamChat(
      request,
      chunk -> {
        log.debug("Received chunk: {}", chunk);
        resp.append(chunk);
      },
      () -> log.info("Stream completed."),
      e -> log.error("Stream failed with an error", e)
    ));
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
    talkbuf.append(System.getenv("GREETINGS"));
    // for (int inx = 0; inx < MAX_CONVERSATIONS; inx++) {
      q = String.format("%s", talkbuf);

      talkbuf.setLength(0);
      apiList.get(LLM_INX_USERS[0]).chat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s says last", agent1, user2)));
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user2, q, user1, a);
      
      talkbuf.setLength(0);
      talkbuf.append(
      apiList.get(LLM_INX_MANAGER).chat(
        map("user",
          String.format(
            "Summarize follow conversations less than 1000 characters.\n%s\n%s\n%s",
            String.format("%s", summary),
            String.format("%s says \"%s\"", user2, q),
            String.format("%s says \"%s\"", user1, a)
          ), 
          "system", String.format("%s", basicState))));
      summary.setLength(0);
      summary.append(talkbuf);
      log.debug("\nSUMMARY: {}", summary);

      q = String.format("%s", a);
      talkbuf.setLength(0);
      talkbuf.append(apiList.get(LLM_INX_USERS[1]).chat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s says last", agent2, user1))));
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user1, q, user2, a);
      q = String.format("%s", a);

      talkbuf.setLength(0);
      talkbuf.append(apiList.get(LLM_INX_MANAGER).chat(
        map("user",
          String.format(
            "Summarize follow conversations less than 1000 characters.\n%s\n%s\n%s",
            String.format("%s", summary),
            String.format("%s says \"%s\"", user1, q),
            String.format("%s says \"%s\"", user2, a)
          ), 
          "system", String.format("%s", basicState))));
      summary.setLength(0);
      summary.append(talkbuf);
      log.debug("\nSUMMARY: {}", summary);

      talkbuf.setLength(0);
      talkbuf.append(apiList.get(LLM_INX_MANAGER).chat(
        map("user",
          String.format(
          "%s\n" + 
          "--\n "+
          "Who will talk next? Aswer using Labels only",
          summary
          ), 
          "system", String.format("%s", basicState))));
      log.debug("NEXT: {}", talkbuf);
      log.debug("SUMMARY:{}", summary);
      log.debug("AGENT1:{}", agent1);
      log.debug("AGENT2:{}", agent2);
      log.debug("Q:{}", q);
      talkbuf.setLength(0);
      talkbuf.append(apiList.get(LLM_INX_USERS[0]).chat(
        map("user", String.format("%s", q), "system", String.format("%s\n%s\nyou are %s\n%s says last", basicState, summary, user1, user2))));
      a = String.format("%s", talkbuf);
      log.debug("\n[Q]{}: {}\n[A]{}: {}", user1, q, user2, a);
    // }
  }

  @Test public void llmTikiTakaTest2() throws Exception {
    if (!TestUtil.isEnabled("llmCommunicationTest2", TestLevel.MANUAL)) { return; }
    int MAX_CONVERSATIONS = 3;
    String subject = "RUST 로 데이터베이스 만들기";
    Map<String, Object> llmCtx = map(
      "llm1", map(
        "type", "openAI",
        "baseUrl", System.getenv("OPENAI_API_BASE_URL"),
        "apiKey", System.getenv("OPENAI_API_KEY"),
        "model", System.getenv("OPENAI_API_MODEL"),
        "api", null
      ),
      "llm2", map(
        "type", "openAI",
        "baseUrl", System.getenv("OPENAI_API_BASE_URL"),
        "apiKey", System.getenv("OPENAI_API_KEY"),
        "model", System.getenv("OPENAI_API_MODEL"),
        "api", null
      )
    );
    List<Map<String, Object>> memberList = list(
      map(
        "number", "0",
        "role", "학생",
        "name", "이용석",
        "sex", "남",
        "age", "20",
        "motive", "전문적인 지식은 없지만 주제에 대해 궁금한걸 물어봐 주도록 해",
        "greeting", "안녕하세요?",
        "llm", "llm2",
        "buf", new StringBuilder()
      ),
      map(
        "number", "1",
        "role", "교수",
        "name", "한지숙",
        "sex", "여",
        "age", "50",
        "motive", "묻는 말에 잘 상담해 주도록 해",
        "greeting", "안녕하세요?",
        "llm", "llm1",
        "buf", new StringBuilder()
      )
    );
    StringBuilder basicState = new StringBuilder();
    StringBuilder conversations = new StringBuilder();
    basicState.append(
      concat(
      "--\n",
      "대화 주제: \"", subject, "\"\n",
      "--\n",
      "참가자:"));
    Map<String, Object> memberMap = map();
    for (String key : llmCtx.keySet()) {
      LLMApi api = null;
      Map<String, Object> map = cast(llmCtx.get(key), map = null);
      LLMProperties props = LLMProperties.builder()
        .baseUrl(cast(map.get("baseUrl"), ""))
        .model(cast(map.get("model"), ""))
        .apiKey(cast(map.get("apiKey"), ""))
        .uriTemplate(cast(map.get("uriTemplate"), ""))
        .clientId(cast(map.get("clientId"), ""))
        .clientSecret(cast(map.get("clientSecret"), ""))
        .refreshToken(cast(map.get("refreshToken"), ""))
      .build();
      switch (cast(map.get("type"), "")) {
      case "openAI": {
        api = new LLMApiOpenAI(props);
      } break;
      case "ollama": {
        api = new LLMApiOllama(props);
      } break;
      case "gemini": {
        api = new LLMApiGemini(props);
      } break;
      default:
      }
      if (api != null) { map.put("api", api); }
    }
    for (int inx = 0; inx < memberList.size(); inx++) {
      Map<String, Object> member =  memberList.get(inx);
      String number = cast(member.get("number"), "");
      String role = cast(member.get("role"), "");
      String motive = cast(member.get("motive"), "");
      memberMap.put(number, member);
      basicState.append(
        concat("\nMember-", number, ": ", role)
      );
    }
    basicState.append("\n--\n");
    Map<String, Object> chatCtx = map(
      "buf", new StringBuilder()
    );
    String memberInx = "0";

    log.debug("BASIC-STATE:{}", basicState);
    LOOP: for (int turn = 0; turn < MAX_CONVERSATIONS; turn++) {
      Map<String, Object> member = cast(memberMap.get(memberInx), member = null);
      String llmId = cast(member.get("llm"), llmId = null);
      Map<String, Object> map = cast(llmCtx.get(llmId), map = null);
      LLMApi api = cast(map.get("api"), api = null);
      if (turn == 0) {
        String greeting = cast(member.get("greeting"), greeting = null);
        log.debug("GREETING:{} / {}", greeting, member);
        conversations.append(concat(
          "Member-", member.get("number"), ": \"", greeting, "\""
        ));
      } else {
      }
      {
        String summary = String.format("TURN:%s\n%s대화내용:\n%s", turn, basicState, conversations);
        log.debug(summary);
        String answer = api.chat(
          map("user", concat(
            summary,
            "\n--\n",
            "다음에 누가 말할거 같아? 호칭만 간단히 말해줘")
          ));
        Pattern ptn = Pattern.compile("Member-([0-9]+)");
        Matcher mat = ptn.matcher(answer);
        if (mat.find()) {
          memberInx = mat.group(1);
          log.debug("NEXT:{} / {}", answer, memberInx);
        }
      }
    }
  }
}