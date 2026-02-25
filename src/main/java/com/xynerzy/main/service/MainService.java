/**
 * @File        : MainService.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Main Service
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.main.service;

import com.xynerzy.system.runtime.AppException;

public interface MainService {
  default void ping() throws AppException { }
  default Object main() throws AppException { return null; }
}
