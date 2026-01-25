/**
 * @File        : MemberEntity.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Member Entity
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.member.control;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xynerzy.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestController @RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberApiControl {

  private final MemberService memberService;
  
  @PostMapping(path = "list")
  public Object memberList() {
    return memberService.memberList();
  }
}
