/**
 * @File        : RemoteFile.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Java Common Constants
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.ReflectionUtil.cast;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;

public interface RemoteFile<T extends RemoteFile<?>> {
  default public String getName() {
    String ret = "";
    return ret;
  }

  default public String getAbsolutePath() {
    String ret = "";
    return ret;
  }

  default public String getPath() {
    String ret = "";
    return ret;
  }

  default public T getParentFile() {
    T ret = null;
    return ret;
  }

  default public T getAbsoluteFile() {
    T ret = null;
    return ret;
  }

  default public boolean exists() {
    boolean ret = false;
    return ret;
  }

  default public URI toURI() {
    URI ret = null;
    return ret;
  }

  default public boolean isFile() {
    boolean ret = false;
    return ret;
  }

  default public boolean isDirectory() {
    boolean ret = false;
    return ret;
  }

  default public long lastModified() {
    long ret = -1;
    return ret;
  }

  default public long length() {
    long ret = -1;
    return ret;
  }

  default public T[] listFiles() {
    T[] ret = cast(new Object[] {}, ret = null);
    return ret;
  }

  default public T[] listFiles(FilenameFilter filter) {
    T[] ret = cast(new Object[] {}, ret = null);
    return ret;
  }

  public File getRealFile();
  public void saveRemoteFile(File file);
}