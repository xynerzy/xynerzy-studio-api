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

  private final LLMProperties props;
  // private final WebClient.Builder webClientBuilder;

  @Override
  public LinkedBlockingQueue<Object> streamChat(Map<String, Object> request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    CoreSystem.executeBackground(() -> {
      String baseUrl = props.getBaseUrl();
      String template = props.getUriTemplate();
      if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
      if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }
      // final String TEMPLATE = template;
      List<Map<String, Object>> parts = new ArrayList<>();
      // for (String k : request.keySet()) {
        parts.add(map(
          "role", "user",
          "parts", 
          list(
            map("text", request.get("user"))
          )
        ));
      // }
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
      // GEMINI-REQUEST:{"contents":[{"role":"user","parts":[{"text":"Hello! Who are you?"}]}]}
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
        try {
          ostream = con.getOutputStream();
          ostream.write(req.getBytes(UTF8));
        } finally {
          safeclose(ostream);
        }
        log.info("START-REQUEST...");
        try {
          istream = con.getInputStream();
          reader = new InputStreamReader(istream, UTF8);
          // {
          //   java.io.BufferedReader breader = new java.io.BufferedReader(reader);
          //   StringBuilder sb = new StringBuilder();
          //   for (String rl; (rl = breader.readLine()) != null;) { sb.append(rl).append("\n"); }
          //   breader.close();
          //   reader.close();
          //   log.debug("RESULT:{}", sb);
          //   reader = new java.io.StringReader(String.valueOf(sb));
          // }
          int depth = 0;
          JsonFactory factory = new JsonFactory();
          JsonParser parser = factory.createParser(reader);
          ObjectMapper mapper = new ObjectMapper();
          List<String> keys = new ArrayList<>();
          String key = "";

          while (parser.nextToken() != null) {
            JsonToken token = parser.currentToken();
            log.debug("TOKEN:{}", token);
            switch (token) {
            case FIELD_NAME: {
              key = parser.getValueAsString();
            } break;
            case START_OBJECT: {
              if (depth > 0) {
                keys.add(key);
                log.debug("KEYS[{}]:{}", depth, keys);
                if (String.valueOf(keys).endsWith("candidates, 0, content, parts, 0]")) {
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
          // LOOP: for (String rl; (rl = reader.readLine()) != null; cnt++) {
          //   log.debug(rl);
          //   // String data = "";
          //   // if (rl.startsWith("data:")) {
          //   //   data = rl.substring(5).trim();
          //   // } else if (rl.startsWith("\u007b")) {
          //   //   /* {:007b ... }:007d */ 
          //   //   data = rl;
          //   // }
          //   // if ("[DONE]".equals(data)) {
          //   //   onNext.accept("\0\0");
          //   //   // onComplete.run();
          //   //   ret.add(Boolean.TRUE);
          //   //   break LOOP;
          //   // }
          //   // if (data == null || "".equals(data)) { continue LOOP; }
          //   // try {
          //   //   log.trace("DATA:{}", data);
          //   //   JSONObject json = new JSONObject(data);
          //   //   String content = json.getJSONArray("choices")
          //   //     .getJSONObject(0)
          //   //     .getJSONObject("delta")
          //   //     .optString("content");
          //   //   if (content != null && !content.isEmpty()) {
          //   //     onNext.accept(content);
          //   //   }
          //   // } catch (Exception e) {
          //   //   onNext.accept("\0\0");
          //   //   ret.add(e);
          //   //   log.error("Error parsing stream data: {}", data, e);
          //   // }
          // }
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
    });
    // {
    // WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
    // GeminiRequest geminiRequest = createGeminiRequest(request);
    // // try {
    // //   log.debug("GEMINI-REQUEST:{}", new ObjectMapper().writeValueAsString(geminiRequest));
    // // } catch (Exception ignore) { }
    // // GEMINI-REQUEST:{"contents":[{"role":"user","parts":[{"text":"Hello! Who are you?"}]}]}
    // final String TEMPLATE = template;
    // webClient.post()
    //   .uri(uriBuilder -> uriBuilder
    //     .path(TEMPLATE.replaceAll("\\$\\{MODEL\\}", props.getModel()))
    //     .queryParam("key", props.getApiKey())
    //     .build())
    //   .bodyValue(geminiRequest)
    //   .retrieve()
    //   .bodyToFlux(GeminiResponse.class)
    //   .doOnNext(response -> {
    //     // log.debug("NEXT:{}", response);
    //     String text = response.extractText();
    //     if (!text.isEmpty()) {
    //       onNext.accept(text);
    //     }
    //   })
    //   .doOnComplete(() -> {
    //     onComplete.run();
    //     ret.add(Boolean.TRUE);
    //   })
    //   .doOnError(e -> {
    //     onError.accept(e);
    //     ret.add(e);
    //   })
    //   .subscribe();
    // }
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