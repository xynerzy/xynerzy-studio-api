/**
 * @File        : LLMApiBase.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM API Base Interface
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/* LLM API Base Interface */
public interface LLMApi {
  /* Process streaming messages */
  default LinkedBlockingQueue<Object> streamChat(Map<String, Object> msg, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) { return null; }
}