/**
 * @File        : LLMApiOllama.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Ollama API
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.Constants.CONTENT_TYPE;
import static com.xynerzy.commons.Constants.CTYPE_JSON;
import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.DataUtil.map;
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
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.xynerzy.system.runtime.CoreSystem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiOllama implements LLMApi {

  private final LLMProperties props;
  private long lastRequestTime;

  @Override public BlockingQueue<Object> streamChat(
    Map<String, Object> rqst,
    Consumer<String> onNext,
    Runnable onComplete,
    Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    long DELAY = 200;
    int MAX_RETRY = 3;
    CoreSystem.executeBackground(() -> {
      long timeDiff = System.currentTimeMillis() - lastRequestTime;
      Map<String, Object> rqmap = map(
        "model", props.getModel(),
        "prompt", cast(rqst.get("user"), ""),
        "system", cast(rqst.get("system"), ""),
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
          URL url = new URL(String.format("%s%s", props.getBaseUrl(), "/api/generate"));
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          /* Set the connection timeout to 5 seconds. */
          con.setConnectTimeout(50000);
          con.setReadTimeout(50000);
          con.setRequestMethod("POST");
          con.setDoInput(true);
          con.setDoOutput(true);
          con.setInstanceFollowRedirects(true);
          con.setRequestProperty(CONTENT_TYPE, CTYPE_JSON);
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
            long ltime = System.currentTimeMillis();
            reader = Channels.newReader(
              rchnl = Channels.newChannel(istrm = con.getInputStream()), UTF8);
            bfrdr = new BufferedReader(reader);
            LOOP: for (String rl; (rl = bfrdr.readLine()) != null;) {
              // log.debug(rl);
              // if (cnt > 10) {
              //   con.disconnect();
              //   break LOOP;
              // }
              String data = "";
              if (rl.startsWith("\u007b")) {
                /* {:007b ... }:007d */ 
                data = rl;
              }
              if (data == null || "".equals(data)) { continue LOOP; }
              try {
                log.trace("DATA:{}", data);
                JSONObject json = new JSONObject(data);
                String content = json.optString("response");
                if (json.optBoolean("done", false)) {
                  break LOOP;
                }
                if (content != null && !content.isEmpty()) {
                  onNext.accept(content);
                  long ctime = System.currentTimeMillis();
                  if (ctime - ltime > 3000) {
                    log.debug("STILL RESPONDING..");
                    ltime = ctime;
                  }
                }
              } catch (Exception e) {
                onNext.accept("\0\0");
                ret.add(true);
                log.error("Error parsing stream data: {}", data, e);
              }
              continue LOOP;
            }
            onComplete.run();
            ret.add(true);
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
          ret.add(true);
        }
        if (retry == MAX_RETRY - 1) {
          ret.add(true);
        }
      }
    });
    return ret;
  }
}