/**
 * @File        : LLMApiOpenAI.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of OpenAI API (ChatGPT, LM-Studio)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.IOUtil.safeclose;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiOpenAI implements LLMApiBase {

  private final LLMProperties props;
  private final WebClient.Builder webClientBuilder;

  @Override public LinkedBlockingQueue<Object> streamChat(
    Map<String, String> request,
    Consumer<String> onNext,
    Runnable onComplete,
    Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    LLMProperties openAIProps = props;

    /* Set the connection timeout to 5 seconds. */
    // HttpClient httpClient = HttpClient.create()
    //   .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

    // WebClient webClient = webClientBuilder
    //   .clientConnector(new ReactorClientHttpConnector(httpClient))
    //   .baseUrl(openAIProps.getBaseUrl())
    //   .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProps.getApiKey())
    //   .build();
    new Thread(() -> {
      List<Map<String, String>> parts = new ArrayList<>();
      for (String k : request.keySet()) {
        parts.add(Map.of("role", k, "content", request.get(k)));
      }
      Map<String, Object> requestBody = Map.of(
        "model", openAIProps.getModel(),
        "messages", parts,
        "temperature", 0.7,
        "top_p", 0.9,
        "repeat_penalty", 1.2,
        "num_predict", 1024,
        // "max_tokens", 512,
        "stream", true
      );
      try {
        URL url = new URL(String.format("%s%s", openAIProps.getBaseUrl(), "/chat/completions"));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String req = new JSONObject(requestBody).toString();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        if (openAIProps.getApiKey() != null) {
          con.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProps.getApiKey());
        }
        OutputStream ostream = null;
        InputStream istream = null;
        InputStreamReader rstream = null;
        BufferedReader reader = null;
        try {
          ostream = con.getOutputStream();
          ostream.write(req.getBytes(StandardCharsets.UTF_8));
        } finally {
          safeclose(ostream);
        }
        try {
          istream = con.getInputStream();
          rstream = new InputStreamReader(istream, StandardCharsets.UTF_8);
          reader = new BufferedReader(rstream);
          int cnt = 0;
          LOOP: for (String rl; (rl = reader.readLine()) != null; cnt++) {
            log.debug(rl);
            // if (cnt > 10) {
            //   /* 강제 종료 */
            //   con.disconnect();
            //   break;
            // }
            /**
             * The stream response comes in over multiple lines in the format "data: { ... }" 
             * When the response is complete, the message "data: [DONE]" is displayed.
             **/
            String data = "";
            if (rl.startsWith("data:")) {
              data = rl.substring(5).trim();
            } else if (rl.startsWith("\u007b")) {
              /* {:007b ... }:007d */ 
              data = rl;
            }
            if ("[DONE]".equals(data)) {
              onNext.accept("\0\0");
              // onComplete.run();
              ret.add(Boolean.TRUE);
              break LOOP;
            }
            if (data == null || "".equals(data)) { continue LOOP; }
            try {
              log.debug("DATA:{}", data);
              JSONObject json = new JSONObject(data);
              String content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("delta")
                .optString("content");
              if (content != null && !content.isEmpty()) {
                onNext.accept(content);
              }
            } catch (Exception e) {
              onNext.accept("\0\0");
              ret.add(e);
              log.error("Error parsing stream data: {}", data, e);
            }
          }
        } finally {
          try { con.disconnect(); } catch (Exception ignore) { }
          safeclose(reader);
          safeclose(rstream);
          safeclose(istream);
        }
      } catch (Exception e) {
        log.warn("E:", e);
      }
    }).start();
    {
      // webClient.post()
      //   .uri("/chat/completions")
      //   .contentType(MediaType.APPLICATION_JSON)
      //   .bodyValue(requestBody)
      //   .retrieve()
      //   .bodyToFlux(String.class)
      //   .doOnNext(line -> {
      //     /**
      //      * The stream response comes in over multiple lines in the format "data: { ... }" 
      //      * When the response is complete, the message "data: [DONE]" is displayed.
      //      **/
      //     String data = "";
      //     if (line.startsWith("data:")) {
      //       data = line.substring(5).trim();
      //     } else if (line.startsWith("\u007b")) {
      //       /* {:007b ... }:007d */ 
      //       data = line;
      //     } else if ("[DONE]".equals(line)) {
      //       onNext.accept("\0\0");
      //       // onComplete.run();
      //       ret.add(Boolean.TRUE);
      //       return;
      //     }
      //     try {
      //       JSONObject json = new JSONObject(data);
      //       String content = json.getJSONArray("choices")
      //         .getJSONObject(0)
      //         .getJSONObject("delta")
      //         .optString("content");
      //       if (content != null && !content.isEmpty()) {
      //         onNext.accept(content);
      //       }
      //     } catch (Exception e) {
      //       onNext.accept("\0\0");
      //       ret.add(e);
      //       log.error("Error parsing stream data: {}", data, e);
      //     }
      //   })
      //   .doOnComplete(onComplete)
      //   .doOnError(onError)
      //   .subscribe();
    }
    return ret;
  }
}
