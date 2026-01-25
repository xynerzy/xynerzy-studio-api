/**
 * @File        : MemberService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2026-01-25
 * @Description : Member Service 
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.member.service;

import java.util.List;

import com.xynerzy.member.entity.MemberEntity.Member;

public interface MemberService {
  default List<Member> memberList() { return null; };
}
