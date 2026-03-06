/**
 * @File        : LLMApiOpenAI.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of OpenAI API (ChatGPT, LM-Studio)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.IOUtil.safeclose;
import static com.xynerzy.commons.ReflectionUtil.cast;

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

import com.xynerzy.system.runtime.CoreSystem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiOpenAI implements LLMApiBase {

  private final LLMProperties props;

  @Override public LinkedBlockingQueue<Object> streamChat(
    Map<String, Object> request,
    Consumer<String> onNext,
    Runnable onComplete,
    Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    CoreSystem.executeBackground(() -> {
      List<Map<String, String>> parts = new ArrayList<>();
      for (String k : request.keySet()) {
        parts.add(Map.of("role", k, "content", cast(request.get(k), "")));
      }
      Map<String, Object> requestBody = Map.of(
        "model", props.getModel(),
        "messages", parts,
        "temperature", 0.7,
        "top_p", 0.9,
        "repeat_penalty", 1.2,
        "num_predict", 1024,
        // "max_tokens", 512,
        "stream", true
      );
      try {
        URL url = new URL(String.format("%s%s", props.getBaseUrl(), "/chat/completions"));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        /* Set the connection timeout to 5 seconds. */
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        String req = new JSONObject(requestBody).toString();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        if (props.getApiKey() != null) {
          con.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey());
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
            //   con.disconnect();
            //   break LOOP;
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
              break LOOP;
            }
            if (data == null || "".equals(data)) { continue LOOP; }
            try {
              log.trace("DATA:{}", data);
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
          onComplete.run();
          ret.add(Boolean.TRUE);
        } finally {
          try { con.disconnect(); } catch (Exception ignore) { }
          safeclose(reader);
          safeclose(rstream);
          safeclose(istream);
        }
      } catch (Exception e) {
        log.warn("E:", e);
        onError.accept(e);
        ret.add(Boolean.TRUE);
      }
    });
    return ret;
  }
}
