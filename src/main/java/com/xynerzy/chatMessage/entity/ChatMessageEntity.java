/**
 * @File        : MessageEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.chatMessage.entity;

import lombok.Builder;
import lombok.Data;

public class ChatMessageEntity {
  @Builder @Data
  public static class ChatMessage {
    private String type;
    private String content;
    private String avatar;
    private String time;
    private String userId;
    private Integer unread;
  }
}
