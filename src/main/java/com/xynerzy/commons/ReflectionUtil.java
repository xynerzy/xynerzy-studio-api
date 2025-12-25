/**
 * @File        : ReflectionUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Java Reflection utility
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.DataUtil.parseBoolean;
import static com.xynerzy.commons.DataUtil.parseByte;
import static com.xynerzy.commons.DataUtil.parseDouble;
import static com.xynerzy.commons.DataUtil.parseFloat;
import static com.xynerzy.commons.DataUtil.parseInt;
import static com.xynerzy.commons.DataUtil.parseLong;
import static com.xynerzy.commons.DataUtil.parseShort;
import static com.xynerzy.commons.StringUtil.camelCase;
import static com.xynerzy.commons.StringUtil.capitalize;
import static com.xynerzy.commons.StringUtil.concat;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionUtil {

  public static Class<?>[] EMPTY_CLS = new Class<?>[] { };
  public static Object[] EMPTY_OBJ = new Object[] { };

  public static Class<?>[] UNARY_CLS_INT = new Class<?>[] { int.class };
  public static Class<?>[] UNARY_CLS_STRING = new Class<?>[] { String.class };

  public static final Class<?> findClass(String clsname) throws Exception { return Class.forName(clsname.trim()); }
  public static final Class<?> findClass(String clsname, ClassLoader loader) throws Exception { return Class.forName(clsname.trim(), true, loader); }
  public static final Method findMethod(Class<?> cls, String name, Class<?>... arg) throws Exception {
    try { return cls.getDeclaredMethod(name.trim(), arg); } catch (Throwable ignore) { };
    try { return cls.getMethod(name.trim(), arg); } catch (Throwable ignore) { };
    return null;
  }
  public static final Method findMethod(Class<?> cls, String name) throws Exception {
    Method ret = null;
    if (name == null || "".equals(name)) { return ret; }
    Method[] methods = null;
    try {
      methods = cls.getMethods();
      for (int inx = 0; inx < methods.length; inx++) {
        if (methods[inx].getName().equals(name)) {
          ret = methods[inx];
          break;
        }
      }
    } catch (Throwable e) { log.trace("E:", e); }
    if (ret != null) { return ret; }
    try {
      methods = cls.getDeclaredMethods();
      for (int inx = 0; inx < methods.length; inx++) {
        if (methods[inx].getName().equals(name)) {
          ret = methods[inx];
          break;
        }
      }
    } catch (Throwable e) { log.trace("E:", e); }
    return ret;
  }
  public static final Field findField(Class<?> cls, String name) throws Exception {
    try { return cls.getDeclaredField(name.trim()); } catch (Throwable ignore) { };
    try { return cls.getField(name.trim()); } catch (Throwable ignore) { };
    return null;
  }
  public static final Object findFieldValue(Class<?> cls, String name) throws Exception { return findFieldValue(cls, name.trim(), null); }
  public static final Object findFieldValue(Class<?> cls, String name, Object inst) throws Exception { return findField(cls, name.trim()).get(inst); }
  public static final Constructor<?> findConstructor(Class<?> cls) throws Exception { return cls.getDeclaredConstructor(EMPTY_CLS); }
  public static final Constructor<?> findConstructor(Class<?> cls, Class<?>... arg) throws Exception { return cls.getDeclaredConstructor(arg); }
  public static final Object newInstance(Class<?> cls) throws Exception { return findConstructor(cls).newInstance(EMPTY_OBJ); }

  public static Method getGetterMethod(Class<?> type, String key) {
    Method ret = null;
    try {
      if (key == null || type == null || "".equals(key)) {
        return ret;
      }
      String mname = concat("get", capitalize(camelCase(key)));
      for (Method m : type.getMethods()) {
        if (mname.equals(m.getName()) && m.getParameterCount() == 0) {
          ret = m;
          break;
        }
      }
    } catch (Exception e) { log.info("E:{}", e); }
    return ret;
  }

  public static Object invokeGetter(Object inst, String key) {
    Object ret = null;
    if (inst == null) { return ret; }
    if (key == null || "".equals(key)) { return ret; }
    Method getter = getGetterMethod(inst.getClass(), key);
    try { ret = getter.invoke(inst); } catch (Exception e) { log.info("E:{}", e); }
    return ret;
  }

  public static Method getSetterMethod(Class<?> type, String key) {
    Method ret = null;
    try {
      if (key == null || type == null || "".equals(key)) { return ret; }
      String mname = concat("set", capitalize(camelCase(key)));
      for (Method m : type.getMethods()) {
        if (mname.equals(m.getName()) && m.getParameterCount() == 1) {
          ret = m;
          break;
        }
      }
    } catch (Exception e) { log.info("E:{}", e); }
    return ret;
  }

  public static void invokeSetter(Object inst, String key, Object val) {
    if (inst == null) { return; }
    if (key == null || "".equals(key)) { return; }
    Method setter = getSetterMethod(inst.getClass(), key);
    try { setter.invoke(inst, val); } catch (Exception e) { log.info("E:{}", e); }
  }

  public static boolean isAssignable(Class<?> target, Class<?>... classes) {
    if (target == null) { return false; }
    for (Class<?> cls : classes) {
      if (cls != null && cls.isAssignableFrom(target)) {
        return true;
      }
    }
    return false;
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

  public static Object parsePrimeType(Class<?> type, Object val) {
    Object ret = null;
    Object tmp = null;
    if (
      ((type == int.class || type == Integer.class)
        && (tmp = parseInt(val, null)) != null) ||
      ((type == long.class || type == Long.class)
        && (tmp = parseLong(val, null)) != null) ||
      ((type == short.class || type == Short.class)
        && (tmp = parseShort(val, null)) != null) ||
      ((type == byte.class || type == Byte.class)
        && (tmp = parseByte(val, null)) != null) ||
      ((type == float.class || type == Float.class)
        && (tmp = parseFloat(val, null)) != null) ||
      ((type == double.class || type == Double.class)
        && (tmp = parseDouble(val, null)) != null) ||
      ((type == boolean.class || type == Boolean.class)
        && (tmp = parseBoolean(val, null)) != null)) {
      ret = tmp;
    } else  if (type == String.class) {
      ret = String.valueOf(val);
    }
    return ret;
  }

  public static Object[] newArray(Class<?> cls, int len) {
    Object[] ret = null;
    ret = cast(Array.newInstance(cls, len), ret);
    return ret;
  }
  
  public static Object[] toArray(List<?> lst, Class<?> cls) {
    Object[] ret = newArray(cls, lst.size());
    ret = lst.toArray(ret);
    return ret;
  }

  public static boolean isPrimeType(Object v) {
    return v == null ? false : isPrimeType(v.getClass());
  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(Object from, T to) {
    try {
      to = (T) from;
    } catch (ClassCastException ignore) { log.trace("E:{}", ignore); }
    return to;
  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(Object from, Class<T> clsTo) {
    T ret = null;
    try {
      ret = (T) from;
    } catch (ClassCastException ignore) { log.trace("E:{}", ignore); }
    return ret;
  }

  public static <T> T copyOf(T src, T tgt) {
    T ret = tgt;
    if (src == null) { return ret; }
    Class<?> cls = src.getClass();
    Field[] fields = cls.getDeclaredFields();
    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object v = field.get(src);
        field.set(tgt, v);
        log.trace("SET:{} / {} / {}", field, v, tgt);
      } catch (Exception ignore) { log.debug("E:{}", ignore); }
    }
    return ret;
  }
}
