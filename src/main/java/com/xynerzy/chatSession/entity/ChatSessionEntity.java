/**
 * @File        : ChatSessionEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : ChatSession Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatSession.entity;

import java.util.List;

import lombok.Builder;
import lombok.Data;

public class ChatSessionEntity {
  @Builder @Data
  public static class ChatSession {
    private String intro;
    private String name;
    private List<String> members;
    private Boolean active;
    private Boolean online;
    private String updated;
    private Integer unread;
  }
}
