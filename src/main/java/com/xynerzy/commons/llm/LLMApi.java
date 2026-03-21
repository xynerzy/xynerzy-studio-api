/**
 * @File        : LLMApiBase.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM API Base Interface
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/* LLM API Base Interface */
public interface LLMApi {
  /* Process streaming messages */
  default BlockingQueue<Object> streamChat(Map<String, Object> msg, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) { return null; }
  default String chat(Map<String, Object> msg) {
    StringBuilder ret = new StringBuilder();
    BlockingQueue<Object> latch = streamChat(msg, v -> { ret.append(v); }, () -> { }, e -> { });
    wait(latch);
    return String.valueOf(ret);
  }
  static void wait(BlockingQueue<Object> latch) {
    try {
      while (latch.poll(1000, TimeUnit.MILLISECONDS) == null) { }
    } catch(Exception e) { }
  }
}