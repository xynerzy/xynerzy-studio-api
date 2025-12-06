/**
 * @File        : Application.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Springboot Application Launcher
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.xynerzy.system.runtime.CoreSystem;

@ServletComponentScan @SpringBootApplication
public class Application extends SpringBootServletInitializer {

  public static void init () {
    String profile = System.getProperty("spring.profiles.active");
    if (profile == null || "".equals(profile)) { System.setProperty("spring.profiles.active", "local"); }
    CoreSystem.getInstance();
  }

  /* for WAS container */
  @Override protected SpringApplicationBuilder configure(SpringApplicationBuilder app) {
    init();
    return app.sources(Application.class);
  }

  /* for standalone boot application */
  public static void main(String[] arg) throws Exception {
    init();
    SpringApplication.run(Application.class, arg);
  }
}
