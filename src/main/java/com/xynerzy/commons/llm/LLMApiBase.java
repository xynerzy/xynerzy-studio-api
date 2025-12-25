/**
 * @File        : LLMApiBase.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : LLM API Base Interface
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons.llm;

import java.util.function.Consumer;

/* LLM API Base Interface */
public interface LLMApiBase {
  /* Process streaming messages */
  default void streamChat(String request, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError) { }
}