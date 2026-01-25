/**
 * @File        : MemberServiceImpl.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Implementation of Member Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.member.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xynerzy.member.entity.MemberEntity.Member;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service
public class MemberServiceImpl implements MemberService {

  @PostConstruct public void init() {
    log.trace("INIT:{}", MemberService.class);
  }

  @Override public List<Member> memberList() {
    return List.of(
      Member.builder()
        .name("tester")
        .intro("hello!")
        .members(List.of("/images/test.svg"))
        .active(true)
        .online(true)
        .updated("PM 01:10")
        .unread(0)
      .build()
    );
  }
}