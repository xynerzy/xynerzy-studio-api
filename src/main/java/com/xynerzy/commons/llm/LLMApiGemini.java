/**
 * @File        : LLMApiGemini.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using API-KEY)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.DataUtil.list;
import static com.xynerzy.commons.DataUtil.map;
import static com.xynerzy.commons.IOUtil.safeclose;
import static com.xynerzy.commons.ReflectionUtil.cast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xynerzy.system.runtime.CoreSystem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiGemini implements LLMApiBase {

  private long lastRequestTime;
  private final LLMProperties props;

  @Override
  public LinkedBlockingQueue<Object> streamChat(Map<String, Object> request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    long DELAY = 1000;
    long timeDiff = System.currentTimeMillis() - lastRequestTime;
    if (timeDiff < DELAY) {
      try {
        Thread.sleep(DELAY - timeDiff);
      } catch (Exception e) {
        log.trace("E:", e);
      }
    }
    CoreSystem.executeBackground(() -> {
      String baseUrl = props.getBaseUrl();
      String template = props.getUriTemplate();
      if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
      if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }
      List<Map<String, Object>> parts = new ArrayList<>();
      parts.add(map(
        "role", "user",
        "parts", 
        list(
          map("text", request.get("user"))
        )
      ));
      Map<String, Object> requestBody = map("contents", parts);
      if (request.containsKey("system")) {
        requestBody.put("system_instruction",
          map(
            "parts",
            list(
              map("text", request.get("system"))
            )
          )
        );
      }
      try {
        URL url = new URL(
          String.format("%s%s", baseUrl,
            template.replaceAll("\\$\\{MODEL\\}", props.getModel())
            // props.getApiKey() != null ? "?key=" + props.getApiKey() : ""
          )
        );
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        /* Set the connection timeout to 10 seconds. */
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        String req = new JSONObject(requestBody).toString();
        log.debug("REQUEST-BODY:{}", req);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        if (props.getApiKey() != null) {
          con.setRequestProperty("x-goog-api-key", props.getApiKey());
        }
        OutputStream ostream = null;
        InputStream istream = null;
        Reader reader = null;
        int respcd = -1;
        try {
          ostream = con.getOutputStream();
          ostream.write(req.getBytes(UTF8));
          respcd = con.getResponseCode();
        } finally {
          safeclose(ostream);
        }
        log.info("START-REQUEST[{}]...", respcd);
        switch (respcd) {
        case 429: {
          log.debug("TOO_MANY_REQUESTS..");
          /* TODO: RETRY.. */
        } break;
        case 200: {
        } break;
        default:
        }
        try {
          istream = con.getInputStream();
          reader = new InputStreamReader(istream, UTF8);
          int depth = 0;
          JsonFactory factory = new JsonFactory();
          JsonParser parser = factory.createParser(reader);
          ObjectMapper mapper = new ObjectMapper();
          List<String> keys = new ArrayList<>();
          String key = "";
          while (parser.nextToken() != null) {
            JsonToken token = parser.currentToken();
            // log.debug("TOKEN:{}", token);
            switch (token) {
            case FIELD_NAME: {
              key = parser.getValueAsString();
            } break;
            case START_OBJECT: {
              if (depth > 0) {
                keys.add(key);
                // log.debug("KEYS[{}]:{}", depth, keys);
                if (keys.size() == 6 && "candidates".equals(keys.get(1)) && "0".equals(keys.get(2)) &&
                  "content".equals(keys.get(3)) && "parts".equals(keys.get(4)) && "0".equals(keys.get(5))) {
                  JsonNode node = mapper.readTree(parser);
                  if (keys.size() > 0) { keys.remove(keys.size() - 1); }
                  if (node != null && node.has("text")) {
                    String text = node.get("text").asText().trim();
                    // log.info("TEXT:{}", node);
                    onNext.accept(text);
                  }
                }
              }
              depth += 1;
            } break;
            case END_OBJECT: {
              if (keys.size() > 0) {
                key = keys.remove(keys.size() - 1);
              } else {
                key = "";
              }
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
              if (keys.size() > 0) {
                key = keys.remove(keys.size() - 1);
              } else {
                key = "";
              }
              depth -= 1;
            } break;
            case VALUE_STRING:
            default:
            }
          }
          onComplete.run();
          ret.add(Boolean.TRUE);
        } finally {
          try { con.disconnect(); } catch (Exception ignore) { }
          safeclose(reader);
          safeclose(istream);
        }
      } catch (Exception e) {
        log.warn("E:", e);
        onError.accept(e);
        ret.add(Boolean.TRUE);
      }
      lastRequestTime = System.currentTimeMillis();
    });
    return ret;
  }

  public static GeminiRequest createGeminiRequest(Map<String, Object> request) {
    List<GeminiRequest.Content> contents = new ArrayList<>();
    for (String key : request.keySet()) {
      GeminiRequest.Part part = new GeminiRequest.Part(cast(request.get(key), ""));
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