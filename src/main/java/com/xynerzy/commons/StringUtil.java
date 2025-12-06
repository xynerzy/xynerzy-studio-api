/**
 * @File        : StringUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : String Manipulate utility
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.commons;

import java.util.List;

public class StringUtil {
  public static String concat(Object... arg) {
    String ret = "";
    for (Object a : arg) {
      if (a instanceof String) {
        ret = ret + a;
      } else {
        ret = ret + String.valueOf(a);
      }
    }
    return ret;
  }

  public static String camelCase(String str) {
    String ret = "";
    String[] words = str.split("_");
    for (String word : words) {
      if (ret.length() == 0) {
        ret = word.toLowerCase();
      } else {
        ret += capitalize(word.toLowerCase());
      }
    }
    return ret;
  }

  public static String snakeCase(String str) {
    String ret = "";
    for (int inx = 0; inx < str.length(); inx++) {
      char c = str.charAt(inx);
      if (c >= 'a' && c <= 'z') {
        ret += (char)(c - 32);
      } else {
        ret += "_" + (char)c;
      }
    }
    return ret;
  }
  public static String decapitalize(String str) {
    char c = str.charAt(0);
    if (c >= 'A' && c <= 'Z') {
      c = (char) ((int) c + 32);
      str = c + str.substring(1);
    }
    return str;
  }
  public static String capitalize(String str) {
    char c = str.charAt(0);
    if (c >= 'a' && c <= 'z') {
      c = (char) ((int) c - 32);
      str = c + str.substring(1);
    }
    return str;
  }

  public static String repeatStr(String s, int len) {
    String ret = "";
    for (int inx = 0; inx < len; inx++) { ret += s; }
    return ret;
  }

  public static String strreplace(String src, String find, String replace) {
    String ret = src;
    if (src == null) { return ret; }
    if (find == null) { return ret; }
    if (replace == null) { return ret; }
    int st;
    while(true) {
      if ((st = src.indexOf(find)) == -1) {
        ret = src;
        break;
      }
      src = src.substring(0, st) + replace + src.substring(st + find.length());
    }
    return ret;
  }

  public static String trim(String str) {
    String ret = str;
    if (ret == null) { return ret; }
    ret = ret.trim();
    return ret;
  }

  public static String substring(String str, int st, Integer ed) {
    String ret = str;
    if (str == null) { return ret; }
    if (str.length() < st) { return ret; }
    if (ed != null && str.length() <= ed) { ed = str.length() - 1; }
    if (ed != null) {
      ret = str.substring(st, ed);
    } else {
      ret = str.substring(st);
    }
    return ret;
  }

  public static String join(Object obj, String delim) {
    StringBuilder ret = new StringBuilder();
    if (obj instanceof List) {
      for (Object item : (List<?>)obj) {
        if (ret.length() > 0) { ret.append(delim); }
        ret.append(String.valueOf(item));
      }
    } else if (obj instanceof String[]) {
      for (String item : (String[])obj) {
        if (ret.length() > 0) { ret.append(delim); }
        ret.append(String.valueOf(item));
      }
    }
    return String.valueOf(ret);
  }
  
  public static boolean startsWith(String str, String v) {
    boolean ret = false;
    if (str == null || v == null) { return ret; }
    ret = str.startsWith(v);
    return ret;
  }

  public static boolean endsWith(String str, String v) {
    boolean ret = false;
    if (str == null || v == null) { return ret; }
    ret = str.endsWith(v);
    return ret;
  }

  public static int indexOf(String str, String v) {
    int ret = -1;
    if (str == null || v == null) { return ret; }
    ret = str.indexOf(v);
    return ret;
  }
}
