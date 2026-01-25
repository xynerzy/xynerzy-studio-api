/**
 * @File        : MessageService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Message Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.message.service;

import java.util.List;

import com.xynerzy.message.entity.MessageEntity.Message;

public interface MessageService {
  default List<Message> messageList() { return null; };
}
