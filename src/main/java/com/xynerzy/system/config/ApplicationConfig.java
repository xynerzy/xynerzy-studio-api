/**
 * @File        : ApplicationConfig.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Application config
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.system.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class ApplicationConfig {

  @Configuration @EnableAspectJAutoProxy(exposeProxy = true)
  public static class AspectConfig {
  }

  @Configuration @OpenAPIDefinition 
  public class OpenAPIConfig {
    @Value("${springdoc.server.url:/}") private String svurl;
    @Value("${springdoc.server.description:Default URL}") private String description;
    @Bean OpenAPI customOpenAPI() {
      List<Server> servers = new ArrayList<>();
      Server server = new Server();
      server.setUrl(svurl);
      server.setDescription(description);
      servers.add(server);
      return new OpenAPI()
        .servers(servers)
        .components(new Components())
        .info(new Info().title("Xynerzy AI Cowork Studio")
        );
    }
  }
 
  @Configuration
  public static class PersistentConfig {
  }

  @Configuration @EnableWebSocket @EnableWebSocketMessageBroker
  public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
      /* from client to server */
      registry.setApplicationDestinationPrefixes("/api/pub");
      /* from server to client */
      registry.enableSimpleBroker("/api/sub");
    }

    @Override public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
      /* SockJS connection address : ws://localhost:8080/api/ws */
      registry.addEndpoint("/api/ws")
        .setAllowedOriginPatterns("*")
        /* compatibility */
        .withSockJS()
        .setSuppressCors(true);
    }
  }

  @Configuration public class DynamicResourceConfig implements WebMvcConfigurer {
    @Override public void addResourceHandlers(@NonNull ResourceHandlerRegistry reg) {
      List<String> paths = List.of("file:/tmp/");
      String[] arr = paths.toArray(arr = new String[paths.size()]);
      if (arr == null) { arr = new String[0]; }
      reg.addResourceHandler("/files/**")
        .addResourceLocations(arr)
        .setCachePeriod(0);
    }
  }
}
