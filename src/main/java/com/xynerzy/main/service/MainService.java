/**
 * @File        : MainService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Service
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.service;

import java.util.Map;

import com.xynerzy.system.runtime.AppException;

public interface MainService {
  default Object main() throws AppException { return null; }
  default Object subscribe(String topic) throws AppException { return null; }
  default Object publish(String topic, Map<String,Object> prm) throws AppException { return null; }
}
