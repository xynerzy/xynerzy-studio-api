/**
 * @File        : ApplicationConfig.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Application config
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.system.config;

import static com.xynerzy.commons.Constants.AUTHORIZATION;
import static com.xynerzy.commons.Constants.COOKIE;
import static com.xynerzy.commons.Constants.PTH_API;
import static com.xynerzy.commons.Constants.PTH_PUB;
import static com.xynerzy.commons.Constants.PTH_SUB;
import static com.xynerzy.commons.Constants.PTH_WS;
import static com.xynerzy.commons.StringUtil.concat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Configuration
public class ApplicationConfig {

  public static final String SECURITY_SCHEME_NAME = "bearerAuth";

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
      Components components = new Components();
      components.addSecuritySchemes(SECURITY_SCHEME_NAME,
        new SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
        );
      return new OpenAPI()
        .servers(servers)
        .components(components)
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
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
      registry.setApplicationDestinationPrefixes(PTH_API + PTH_PUB);
      /* from server to client */
      registry.enableSimpleBroker(PTH_API + PTH_SUB);
    }

    @Override public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
      /* SockJS connection address : ws://localhost:8080/api/ws */
      registry.addEndpoint(PTH_API + PTH_WS)
        .setAllowedOriginPatterns("*")
        .addInterceptors(new HandshakeInterceptor() {
          @Override public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
            WebSocketHandler hnd, Map<String, Object> atr) throws Exception {
            log.debug("BEFORE-HANDSHAKE..{} / {} / {} / {} / {}", req.getURI(), req.getHeaders(), req.getHeaders().get(COOKIE), hnd, atr);
            return true;
          }
          @Override public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
            WebSocketHandler hnd, Exception ex) {
            log.debug("AFTER-HANDSHAKE..{} / {} / {} / {} / {}", req.getURI(), req.getHeaders(), req.getHeaders().get(COOKIE), hnd, req.getAttributes());
          }
        })
        /* compatibility */
        .withSockJS()
        // .setWebSocketEnabled(false)
        .setHeartbeatTime(1000);
    }
    @Override public void configureClientInboundChannel(@NonNull ChannelRegistration reg) {
      reg.interceptors(new ChannelInterceptor() {
        @Override public Message<?> preSend(@NonNull Message<?> msg, @NonNull MessageChannel chn) {
          StompHeaderAccessor acc = StompHeaderAccessor.wrap(msg);
          log.debug("CHECK:{}", acc);
          if (acc != null) {
            if (StompCommand.CONNECT.equals(acc.getCommand())) {
              Map<String, Object> atr = acc.getSessionAttributes();
              String[] auth = String.valueOf(acc.getFirstNativeHeader(AUTHORIZATION)).split("[:]");
              if (atr != null) {
                String userId = "";
                String sessionId = "";
                {
                  userId = auth != null && auth.length > 0 ? auth[0] : "";
                  sessionId = auth != null && auth.length > 1 ? auth[1] : "";
                  log.debug("AUTH:{} / {}", auth, atr);
                }
                atr.putAll(Map.of(
                  "userId", userId,
                  "sessionId", sessionId
                ));
              }
            }
          }
          return msg;
        }
      });
    }
    @EventListener public void handleSubscribeEvent(SessionSubscribeEvent evt) {
      StompHeaderAccessor sha = StompHeaderAccessor.wrap(evt.getMessage());
      Map<String, Object> atr = sha.getSessionAttributes();
      String dest = sha.getDestination();
      String sid = "";
      String sbscId = "";
      String userId = "";
      if (sha != null) {
        sid = sha.getSessionId();
        sbscId = concat(sid, sha.getSubscriptionId());
      }
      log.debug("DEST:{} / SUBSCRIBE-ID:{} / USER-ID:{}", dest, sbscId, userId);
      log.debug("SESSION:{} / {} / {}", atr, sha);
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
