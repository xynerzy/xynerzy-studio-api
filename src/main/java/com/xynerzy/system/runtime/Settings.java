/**
 * @File        : Settings.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : System settings
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.system.runtime;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xynerzy.commons.IOUtil.RawBytesInputStream;
import com.xynerzy.commons.IOUtil.RawBytesOutputStream;
import com.xynerzy.commons.IOUtil.Streamable;

import lombok.Getter;

@Component @Getter
public class Settings implements Streamable {
  @Value("${spring.application.name:}") private String appName;
  @Value("${core.thread.pool.size:4}") private int coreThreadMaximumPoolSize;
  @Value("${core.thread.keep-alive-time:600000}") private long coreThreadKeepAliveTime;

  @Override public int readFrom(RawBytesInputStream istream) throws IOException {
    int ret = 0;
    return ret;
  }

  @Override public int writeTo(RawBytesOutputStream ostream) throws IOException {
    int ret = 0;
    return ret;
  }
}
