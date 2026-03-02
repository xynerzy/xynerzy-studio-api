/**
 * @File        : HttpClientUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-02-28
 * @Description : Date Manipulation Utility
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.DataUtil.parseInt;
import static com.xynerzy.commons.DataUtil.parseStr;
import static com.xynerzy.commons.IOUtil.safeclose;
import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleHttpClientUtil {
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String DELETE = "DELETE";
  public static final String PUT = "PUT";
  public static final String CONNECTION = "CONNECTION";
  public static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
  public static final String READ_TIMEOUT = "READ_TIMEOUT";
  public static final String RESULT_HEADERS = "RESULT_HEADERS";
  public static final String RESULT_CODE = "RESULT_CODE";
  public static final String CONTENT_TYPE = "ContentType";
  public static final String APPLICATION_JSON_VALUE = "application/json";
  public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

  public static String simpleHttpRequest(String ustr, String method, Map<String, Object> prm, Map<String, Object> hdr) throws Exception { return simpleHttpRequest(ustr, method, prm, hdr, null); }
  public static String simpleHttpRequest(
    String ustr,
    String method,
    Map<String, Object> prm,
    Map<String, Object> hdr,
    Consumer<String> onNext
    ) throws Exception {
    String ret = null;
    URL url = null;
    HttpURLConnection con = null;
    OutputStream ostream = null;
    InputStream istream = null;
    InputStreamReader rstream = null;
    BufferedReader reader = null;
    String content = null;
    String charset = UTF8;
    String rqCtnType = "";
    byte[] buf = null;
    int rescd = 0;
    int ctmout = 5000;
    int rtmout = 30000;
    String rurl = ustr;
    int errlvl = 0;
    if (hdr != null) {
      if (hdr.containsKey(CONNECTION_TIMEOUT)) {
        ctmout = parseInt(hdr.remove(CONNECTION_TIMEOUT), ctmout);
        log.trace("CONNECTION-TIMEOUT:{}", ctmout);
      }
      if (hdr.containsKey(READ_TIMEOUT)) {
        rtmout = parseInt(hdr.remove(READ_TIMEOUT), rtmout);
        log.trace("READ-TIMEOUT:{}", rtmout);
      }
      if (hdr.containsKey(RESULT_HEADERS)) { hdr.remove(RESULT_HEADERS); }
      if (hdr.containsKey(RESULT_CODE)) { hdr.remove(RESULT_CODE); }
    }
    try {
      switch (method) {
      case PUT:
      case POST: {
        String ctype = "";
        if (hdr != null) {
          LOOP: for (String k : hdr.keySet()) {
            if (k == null) { continue LOOP; }
            Object v = hdr.get(k);
            String hdkey = k.toLowerCase().replaceAll("[ \t\r\n]+", "");
            String hdval = parseStr(v, "").toLowerCase().replaceAll("[ \t\r\n]+", "");
            SW: switch (hdkey) {
            case "content-type": {
              rqCtnType = parseStr(hdr.get(k), null);
              if (hdval.startsWith(APPLICATION_JSON_VALUE)) {
                ctype = APPLICATION_JSON_VALUE;
              } else {
                ctype = APPLICATION_FORM_URLENCODED_VALUE;
              }
            } break SW;
            default: }
            if (k != null) { continue LOOP; }
          }
        }
        switch (ctype) {
        case APPLICATION_JSON_VALUE: {
          // content = new JSONObject(prm).toJSONString();
        } break;
        case APPLICATION_FORM_URLENCODED_VALUE:
        default: {
          content = makeFormData(prm, charset);
        }}
        if (errlvl < 3) { log.debug("CONTENT:{}", content); }
      } break;
      case DELETE:
      case GET: {
        url = new URI(rurl).toURL();
        rurl = concat(url.getProtocol(), "://", url.getHost(), ":", url.getPort(), "");
        String rprot = url.getProtocol();
        String rhost = url.getHost();
        String rport = url.getPort() != -1 ? ":" + url.getPort() : "";
        String rpath = url.getPath();
        String rqstr = url.getQuery();
        String aqstr = makeFormData(prm, charset);
        if (aqstr != null && !"".equals(aqstr)) {
          if (rqstr == null || "".equals(rqstr)) {
            rqstr = aqstr;
          } else {
            rqstr = concat(rqstr, "&", aqstr);
          }
        }
        if (errlvl < 3) { log.debug("CHECK:{} / {} / {} / {} / {}", rprot, rhost, rport, rpath, rqstr); }
        rurl = String.format("%s://%s%s%s%s", rprot, rhost, rport, rpath, rqstr != null && !"".equals(rqstr) ? "?" + rqstr : "");
      } break;
      default: }
      if (errlvl < 2) { log.debug("URL:{} / {} / {} / {}", method, rurl, rqCtnType, hdr); }
      url = new URI(rurl).toURL();
      con = cast(url.openConnection(), con);
      con.setDoInput(true);
      con.setDoOutput(true);
      con.setInstanceFollowRedirects(true);
      con.setRequestMethod(method);
      if (rqCtnType != null) {
        con.setRequestProperty(CONTENT_TYPE, rqCtnType);
      }
      if (hdr != null) {
        for (String k : hdr.keySet()) {
          Object v = hdr.get(k);
          if (v != null && !"".equals(v)) {
            switch (k) {
            case CONNECTION: case CONNECTION_TIMEOUT: case READ_TIMEOUT:
            case RESULT_HEADERS: case RESULT_CODE:
            break;
            default:
              con.setRequestProperty(k, String.valueOf(v));
            }
          }
        }
        hdr.put(CONNECTION, con);
      }
      con.setConnectTimeout(ctmout);
      con.setReadTimeout(rtmout);
      if (content == null) { content = ""; }
      buf = content.getBytes(charset);
      switch(method) {
      case POST: case PUT: {
        ostream = con.getOutputStream();
        ostream.write(buf, 0, buf.length);
        ostream.flush();
      } break; }
      rescd = con.getResponseCode();
      log.trace("RESCD:{}", rescd);
      if (rescd >= 200 & rescd < 300) {
        istream = con.getInputStream();
      } else {
        istream = con.getErrorStream();
      }
      if (hdr != null) {
        // hdr.put(RESULT_HEADERS, con.getHeaderFields());
        // hdr.put(RESULT_CODE, Integer.valueOf(rescd));
      }
      rstream = new InputStreamReader(istream, UTF8);
      reader = new BufferedReader(rstream);
      StringBuilder sb = new StringBuilder();
      for (String rl; (rl = reader.readLine()) != null;) {
        sb.append(rl).append("\n");
        if (onNext != null) { onNext.accept(rl); }
      }
      ret = String.valueOf(sb);
      if (errlvl < 3) { log.debug("RESULT[{}]:{}", rescd, ret); }
    } catch (SocketTimeoutException e) {
      log.info("CHECK-TIMEOUT:{} / {}", ctmout, rtmout);
      switch (errlvl) {
      case 3: { /* NO-OP */ } break;
      case 2: { log.trace("ERROR:{} / {}", rurl, e.getMessage()); } break;
      case 1: { log.info("ERROR:{} / {}", rurl, e.getMessage()); } break;
      case 0:
      default: {
        log.info("ERROR:{} / {}", rurl, e.getMessage());
        throw new RuntimeException(e);
      }}
    } catch (Exception e) {
      switch (errlvl) {
      case 3: { /* NO-OP */ } break;
      case 2: { log.trace("ERROR:{} / {}", rurl, e.getMessage()); } break;
      case 1: { log.info("ERROR:{} / {}", rurl, e.getMessage()); } break;
      case 0:
      default: {
        log.info("ERROR:{} / {}", rurl, e);
        throw new RuntimeException(e);
      }}
    } finally {
      safeclose(istream);
      safeclose(rstream);
      safeclose(ostream);
      safeclose(reader);
      if (con != null) { try { con.disconnect(); } catch (Exception e) { log.trace("E", e); } }
    }
    return ret;
  }

  public static String makeFormData(Map<String, Object> prm, String charset) {
    StringBuilder res = new StringBuilder();
    LOOP: for (String k : prm.keySet()) {
      if (res.length() > 0) { res.append("&"); }
      try {
        if (k != null && !"".equals(k)) {
          res
            .append(URLEncoder.encode(k, charset))
            .append("=")
            .append(URLEncoder.encode(parseStr(prm.get(k)), charset));
        }
      } catch (Exception e) {
        break LOOP;
      }
    }
    return String.valueOf(res).trim();
  }
}