/**
 * @File        : DataUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Java Common Constants
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.ReflectionUtil.invokeGetter;
import static com.xynerzy.commons.ReflectionUtil.isAssignable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataUtil {

  public static String parseStr(Object o) {
    if (o != null) {
      return String.valueOf(o);
    }
    return null;
  }
  public static String parseStr(Object o, String def) {
    String ret = def;
    if (o == null) { return def; }
    ret = parseStr(o);
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Integer parseInt(Object o) { return parseInt(o, null); }
  public static Integer parseInt(Object o, Integer def) {
    Integer ret = def;
    if (o == null) { return def; }
    try { ret = Integer.parseInt(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Long parseLong(Object o) { return parseLong(o, null); }
  public static Long parseLong(Object o, Long def) {
    Long ret = def;
    if (o == null) { return def; }
    try { ret = Long.parseLong(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Float parseFloat(Object o) { return parseFloat(o, null); }
  public static Float parseFloat(Object o, Float def) {
    Float ret = def;
    if (o == null) { return def; }
    try { ret = Float.parseFloat(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Double parseDouble(Object o) { return parseDouble(o, null); }
  public static Double parseDouble(Object o, Double def) {
    Double ret = def;
    if (o == null) { return def; }
    try { ret = Double.parseDouble(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Short parseShort(Object o) { return parseShort(o, null); }
  public static Short parseShort(Object o, Short def) {
    Short ret = def;
    if (o == null) { return def; }
    try { ret = Short.parseShort(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Byte parseByte(Object o) { return parseByte(o, null); }
  public static Byte parseByte(Object o, Byte def) {
    Byte ret = def;
    if (o == null) { return def; }
    try { ret = Byte.parseByte(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  public static Boolean parseBoolean(Object o) { return parseBoolean(o, null); }
  public static Boolean parseBoolean(Object o, Boolean def) {
    Boolean ret = def;
    if (o == null) { return def; }
    try { ret = Boolean.parseBoolean(String.valueOf(o)); } catch (Exception e) { log.trace("E:{}", e); }
    if (ret == null) { ret = def; }
    return ret;
  }
  
  public static Map<String, Object> newMap() { return new LinkedHashMap<>(); }

  @SafeVarargs
  public static <T extends Object> Map<String, T> mergeMap(T... maps) {
    Map<String, T> ret = new LinkedHashMap<>();
    for (Object item : maps) {
      if (item instanceof Map) {
        Map<String, T> map = cast(item, map = null);
        for (String key : map.keySet()) {
          T v1 = map.get(key);
          T v2 = ret.get(key);
          if (v1 != null) {
            if (v1 instanceof Map && v2 instanceof Map) {
              Map<?, ?> m1 = cast(v1, m1 = null);
              Map<?, ?> m2 = cast(v2, m2 = null);
              if (m1 != null && m2 != null) { v1 = cast(mergeMap(m1, m2), v1); }
              ret.put(key, v1);
            } else {
              ret.put(key, v1);
            }
          }
        }
      }
    }
    return ret;
  }

  @SafeVarargs
  public static <T extends Object> List<T> asList(T... arr) {
    List<T> ret = null;
    if (arr != null && arr.length > 0) {
      ret = new ArrayList<>();
      for (T itm : arr) { ret.add(itm); }
    }
    return ret;
  }

  @SafeVarargs
  public static <T> List<T> mergeList(List<T>... lst) {
    List<T> ret = new ArrayList<>();
    for (List<T> itm : lst) { ret.addAll(itm); }
    return ret;
  }

  @SafeVarargs
  public static <T> T[] array(T... arr) { return arr; }

  @SafeVarargs
  public static <T> T[] mergeArray(T[]... arr) {
    T[] ret = null;
    int ginx = 0, len = 0;
    for (T[] itm : arr) { len += itm.length; }
    ret = Arrays.copyOf(arr[0], len);
    for (int inx = 0; inx < arr.length; inx++) {
      T[] itm = arr[inx];
      for (int sinx = 0; sinx < itm.length; sinx++, ginx++) {
        ret[ginx] = itm[sinx];
      }
    }
    return ret;
  }

  @SafeVarargs
  public static <T> List<T> list(T... arr) {
    List<T> ret = new ArrayList<>();
    for (int inx = 0; inx < arr.length; inx++) { ret.add(arr[inx]); }
    return ret;
  }

  public static <T> T arrayValue(T[] arr, int inx) { return arrayValue(arr, inx, null); }
  public static <T> T arrayValue(T[] arr, int inx, T def) {
    T ret = def;
    if (arr == null || inx < 0 || arr.length == 0 || arr.length <= inx) { return ret; }
    ret = arr[inx];
    return ret;
  }

  public static <T> T arraySet(T[] arr, int inx, T v) { return arr[inx] = v; }

  public static List<String> attrAsList(List<?> lst, String colname) { return attrAsList(lst, colname, String.class); }

  public static <O> List<O> attrAsList(List<?> lst, String colname, Class<O> cls) {
    List<O> ret = new ArrayList<>();
    if (lst == null || lst.size() == 0) { return ret; }
    for (Object item : lst) {
      Object o = null;
      if (item != null) {
        Class<?> tcls = (Class<?>)item.getClass();
        if (!isPrimeType(tcls)) {
          o = invokeGetter(item, colname);
          if (isAssignable(cls, String.class)) {
            o = parseStr(o);
          } else if (isAssignable(cls, int.class, Integer.class)) {
            o = parseInt(o, 0);
          } else if (isAssignable(cls, long.class, Long.class)) {
            o = parseLong(o, 0L);
          } else if (isAssignable(cls, float.class, Float.class)) {
            o = parseFloat(o, 0F);
          } else if (isAssignable(cls, double.class, Double.class)) {
            o = parseDouble(o, 0D);
          }
        } else {
          o = item;
        }
      }
      try {
        ret.add(cast(o, cls));
      } catch (ClassCastException e) {
        ret.add(null);
      }
    }
    return ret;
  }
  
  public static boolean isPrimeType(Class<?> type) {
    boolean ret = false;
    if (type == String.class ||
      type == int.class || type == Integer.class ||
      type == long.class || type == Long.class ||
      type == short.class || type == Short.class ||
      type == byte.class || type == Byte.class ||
      type == float.class || type == Float.class ||
      type == double.class || type == Double.class ||
      type == boolean.class || type == Boolean.class) {
      ret = true;
    }
    return ret;
  }
  
  public static Object getCascade(Map<String, Object> map, String... keys) {
    Object ret = null;
    if (map == null) { return ret; }
    if (keys == null) { return ret; }
    Object t = null;
    for (String key : keys) {
      if (key == null) { return ret; }
      t = map.get(key);
      if (t != null && t instanceof Map) {
        map = cast(t, map);
      }
    }
    if (t != null) { ret = t; }
    return ret;
  }
}
