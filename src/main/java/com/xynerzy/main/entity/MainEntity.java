/**
 * @File        : MainEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.entity;

import lombok.Builder;
import lombok.Data;

public class MainEntity {
  @Builder @Data
  public static class Result {
    private String type;
    private String errcd;
    private String resultMsg;
  }
}
