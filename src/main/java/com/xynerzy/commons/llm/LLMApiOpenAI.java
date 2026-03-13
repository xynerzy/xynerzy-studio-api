/**
 * @File        : LLMApiOpenAI.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of OpenAI API (ChatGPT, LM-Studio)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.Constants.CONTENT_TYPE;
import static com.xynerzy.commons.Constants.CTYPE_JSON;
import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.IOUtil.readAsString;
import static com.xynerzy.commons.IOUtil.safeclose;
import static com.xynerzy.commons.ReflectionUtil.cast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
  private long lastRequestTime;

  @Override public LinkedBlockingQueue<Object> streamChat(
    Map<String, Object> rqst,
    Consumer<String> onNext,
    Runnable onComplete,
    Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    long DELAY = 200;
    int MAX_RETRY = 3;
    CoreSystem.executeBackground(() -> {
      long timeDiff = System.currentTimeMillis() - lastRequestTime;
      List<Map<String, String>> parts = new ArrayList<>();
      for (String k : rqst.keySet()) {
        parts.add(Map.of("role", k, "content", cast(rqst.get(k), "")));
      }
      Map<String, Object> rqmap = Map.of(
        "model", props.getModel(),
        "messages", parts,
        "temperature", 0.7,
        "top_p", 0.9,
        // "repeat_penalty", 1.2,
        // "num_predict", 1024,
        // "max_tokens", 512,
        "stream", true
      );
      RETRY: for (int retry = 0; retry < MAX_RETRY; retry++) {
        try {
          if (timeDiff < DELAY) {
            try {
              Thread.sleep(DELAY - timeDiff);
            } catch (Exception e) {
              log.trace("E:", e);
            }
          }
          // log.debug("PROPS:{}", props);
          URL url = new URL(String.format("%s%s", props.getBaseUrl(), "/chat/completions"));
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          /* Set the connection timeout to 5 seconds. */
          con.setConnectTimeout(50000);
          con.setReadTimeout(50000);
          con.setRequestMethod("POST");
          con.setDoInput(true);
          con.setDoOutput(true);
          con.setInstanceFollowRedirects(true);
          con.setRequestProperty(CONTENT_TYPE, CTYPE_JSON);
          if (props.getApiKey() != null) {
            // log.info("API-KEY:{}", props.getApiKey());
            con.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey());
          }
          InputStream istrm = null;
          OutputStream ostrm = null;
          BufferedReader bfrdr = null;
          WritableByteChannel wchnl = null;
          ReadableByteChannel rchnl = null;
          int respcd = -1;
          ByteBuffer btbuf = null;
          Reader reader = null;
          try {
            // log.debug("REQUEST-BODY:{}", rqmap);
            wchnl = Channels.newChannel(ostrm = con.getOutputStream());
            btbuf = ByteBuffer.wrap(new JSONObject(rqmap).toString().getBytes(UTF8));
            wchnl.write(btbuf);
            respcd = con.getResponseCode();
          } finally {
            if (btbuf != null) { btbuf.clear(); }
            safeclose(wchnl);
            safeclose(ostrm);
          }
          log.info("START-REQUEST[{}]...", respcd);
          switch (respcd) {
          case 429: {
            log.debug("TOO_MANY_REQUESTS..");
            continue RETRY;
          }
          case 200: {
          } break;
          default:
            reader = Channels.newReader(
              rchnl = Channels.newChannel(istrm = con.getErrorStream()), UTF8);
            String msg = readAsString(reader);
            log.info("ERR:{}", msg);
            onError.accept(new RuntimeException(msg));
            ret.add(true);
            break RETRY;
          }
          try {
            reader = Channels.newReader(
              rchnl = Channels.newChannel(istrm = con.getInputStream()), UTF8);
            bfrdr = new BufferedReader(reader);
            LOOP: for (String rl; (rl = bfrdr.readLine()) != null;) {
              // log.debug(rl);
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
            break RETRY;
          } finally {
            try { con.disconnect(); } catch (Exception ignore) { }
            safeclose(bfrdr);
            safeclose(reader);
            safeclose(rchnl);
            safeclose(istrm);
          }
        } catch (Exception e) {
          log.warn("E:", e);
          onError.accept(e);
          ret.add(Boolean.TRUE);
        }
        if (retry == MAX_RETRY - 1) {
          ret.add(Boolean.TRUE);
        }
      }
    });
    return ret;
  }
}
