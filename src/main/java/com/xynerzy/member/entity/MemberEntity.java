/**
 * @File        : MemberEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Member Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.member.entity;

import java.util.List;

import lombok.Builder;
import lombok.Data;

public class MemberEntity {
  @Builder @Data
  public static class Member {
    private String intro;
    private String name;
    private List<String> members;
    private Boolean active;
    private Boolean online;
    private String updated;
    private Integer unread;
  }
}
