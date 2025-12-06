/**
 * @File        : MainService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Service
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.main.service;

import java.util.Map;

import com.xynerzy.system.runtime.AppException;

public interface MainService {
  Object main() throws AppException;
  Object subscribe(String topic) throws AppException;
  Object publish(String topic, Map<String,Object> prm) throws AppException;
}
