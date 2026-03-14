/**
 * @File        : LLMApiGeminiOAuth2.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Implementation of Google Gemini API (Using O-Auth2)
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import static com.xynerzy.commons.Constants.CONTENT_TYPE;
import static com.xynerzy.commons.Constants.CTYPE_JSON;
import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.DataUtil.list;
import static com.xynerzy.commons.DataUtil.map;
import static com.xynerzy.commons.IOUtil.readAsString;
import static com.xynerzy.commons.IOUtil.safeclose;

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xynerzy.commons.SimpleHttpClientUtil;
import com.xynerzy.system.runtime.CoreSystem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class LLMApiGeminiOAuth2 implements LLMApi {

  private final LLMProperties props;
  private long lastRequestTime;

  @Override public LinkedBlockingQueue<Object> streamChat(
    Map<String, Object> rqst,
    Consumer<String> onNext,
    Runnable onComplete,
    Consumer<Throwable> onError) {
    LinkedBlockingQueue<Object> ret = new LinkedBlockingQueue<>();
    long DELAY = 1500;
    int MAX_RETRY = 3;
    CoreSystem.executeBackground(() -> {
      long timeDiff = System.currentTimeMillis() - lastRequestTime;
      String baseUrl = props.getBaseUrl();
      String template = props.getUriTemplate();
      if (baseUrl == null || "".equals(baseUrl)) { baseUrl = "https://generativelanguage.googleapis.com"; }
      if (template == null || "".equals(template)) { template = "/v1beta/models/${MODEL}:streamGenerateContent"; }
      List<Map<String, Object>> usr = new ArrayList<>();
      List<Map<String, Object>> sys = new ArrayList<>();
      for (String k : rqst.keySet()) {
        switch (k) {
        case "user": {
          usr.add(map("text", rqst.get("user")));
        } break;
        default:
          sys.add(map("text", rqst.get("user")));
        }
      }
      Map<String, Object> rqmap = map(
        "contents",
        list(map("role", "user", "parts", usr))
      );
      if (sys.size() > 0) {
        rqmap.put(
          "system_instruction",
          map(
            "parts",
            list(map("text", rqst.get("system")))
          )
        );
      }
      /* Issue an access token using a refresh token */
      String accessToken = "";
      try {
        String tkurl = "https://accounts.google.com/o/oauth2/token";
        Map<String, Object> tkprm = map(
          "client_id", props.getClientId(),
          "client_secret", props.getClientSecret(),
          "refresh_token", props.getRefreshToken(),
          "grant_type", "refresh_token"
        );
        String tkres = SimpleHttpClientUtil
          .simpleHttpRequest(tkurl, "POST", tkprm, map());
        JSONObject tkjson = new JSONObject(tkres);
        accessToken = tkjson.optString("access_token", "");
      } catch (Exception e) {
        log.info("Can't issue access-token");
        onError.accept(e);
        ret.add(true);
      }
      RETRY: for (int retry = 0; retry < MAX_RETRY; retry++) {
        try {
          if (timeDiff < DELAY) {
            try {
              Thread.sleep(DELAY - timeDiff);
            } catch (Exception e) {
              log.trace("E:", e);
            }
          }
          URL url = new URL(
            String.format("%s%s", baseUrl,
              template.replaceAll("\\$\\{MODEL\\}", props.getModel())
              // props.getApiKey() != null ? "?key=" + props.getApiKey() : ""
            )
          );
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          /* Set the connection timeout to 10 seconds. */
          con.setConnectTimeout(50000);
          con.setReadTimeout(50000);
          con.setRequestMethod("POST");
          con.setDoInput(true);
          con.setDoOutput(true);
          con.setInstanceFollowRedirects(true);
          con.setRequestProperty(CONTENT_TYPE, CTYPE_JSON);
          if (accessToken != null) {
            log.debug("ACCTOKEN:{}", accessToken);
            /* Call the API using the issued access token. */
            con.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
          }
          InputStream istrm = null;
          OutputStream ostrm = null;
          Reader reader = null;
          WritableByteChannel wchnl = null;
          ReadableByteChannel rchnl = null;
          int respcd = -1;
          ByteBuffer btbuf = null;
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
                      long ctime = System.currentTimeMillis();
                      if (ctime - ltime > 3000) {
                        log.debug("STILL RESPONDING..");
                        ltime = ctime;
                      }
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
            lastRequestTime = System.currentTimeMillis();
            onComplete.run();
            ret.add(true);
            break RETRY;
          } finally {
            try { con.disconnect(); } catch (Exception ignore) { }
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
