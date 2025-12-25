/**
 * @File        : WebUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Spring web utility
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.StringUtil.concat;
import static com.xynerzy.system.runtime.CoreSystem.getGlobal;
import static java.lang.Thread.currentThread;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class WebUtil {
  public static ServletRequestAttributes currentRequestAttributes() {
    return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
  }
  public static HttpServletRequest currentRequest() {
    HttpServletRequest ret = null;
    if (ret == null) { ret = getGlobal(concat(HttpServletRequest.class.getName(), "@", currentThread().getId()), ret); }
    if (ret == null) { ret = currentRequestAttributes().getRequest(); }
    return ret;
  }
  public static HttpServletResponse currentResponse() {
    HttpServletResponse ret = null;
    if (ret == null) { ret = getGlobal(concat(HttpServletResponse.class.getName(), "@", currentThread().getId()), ret); }
    if (ret == null) { ret = currentRequestAttributes().getResponse(); }
    return ret;
  }
  public static HttpSession currentSession() {
    HttpSession ret = null;
    HttpServletRequest req = null;
    if (ret == null) { ret = getGlobal(concat(HttpSession.class.getName(), "@", currentThread().getId()), ret); }
    if (ret == null && (req = getGlobal(concat(HttpServletResponse.class.getName(), "@", currentThread().getId()), req)) != null) { ret = req.getSession(); }
    if (ret == null && (req = currentRequestAttributes().getRequest()) != null) { ret = req.getSession(); }
    return ret;
  }
}
