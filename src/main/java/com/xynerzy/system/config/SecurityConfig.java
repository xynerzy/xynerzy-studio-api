/**
 * @File        : SecurityConfig.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Spring Security Config
 * @Site        : https://github.com/xynerzy
 **/
package com.xynerzy.system.config;

import static com.xynerzy.commons.DataUtil.list;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
  private final CorsFilter corsFilter;
  private final AuthFilter authFilter;
  private final CustomAuthenticationEntryPoint authPoint;
  private final CustomAccessDeniedHandler authHandler;

  /* URL pattern matcher */
  public static AntPathRequestMatcher matcher(HttpMethod m, String path) {
    if (m == null) {
      return new AntPathRequestMatcher(path);
    } else {
      return new AntPathRequestMatcher(path, m.name());
    }
  }

  @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    /* Publicly accessible list */
    List<RequestMatcher> reqPubLst = new ArrayList<>();
    /* Authorized users accessible list */
    List<RequestMatcher> reqMbrLst = new ArrayList<>();
    /* Web Resources */
    List<RequestMatcher> reqWebLst = new ArrayList<>();

    reqPubLst.addAll(list());
    reqMbrLst.addAll(list());
    reqWebLst.addAll(list());


    reqPubLst.addAll(List.of(
      matcher(GET, "/"),
      matcher(GET, "/index.html"),
      matcher(GET, "/favicon.ico"),
      matcher(GET, "/main"),
      matcher(GET, "/error"),
      matcher(GET, "/files/**"),
      matcher(HEAD, "/api/**"),
      matcher(GET, "/api/**"),
      matcher(POST, "/api/**"),
      /* H2DB web console */
      matcher(null, "/h2-console/**"),
      /* Swagger (OPENAPI) */
      matcher(null, "/swagger/swagger-ui/**"),
      matcher(null, "/swagger/swagger-resources/**"),
      matcher(null, "/swagger/v3/api-docs/**"),
      /* Web resources */
      matcher(null, "/**/*.js"),
      matcher(null, "/**/*.css"),
      matcher(null, "/assets/**"),
      matcher(null, "/**/*.html")
    ));

    RequestMatcher[] reqPub = reqPubLst.toArray(new RequestMatcher[]{ });
    RequestMatcher[] reqMbr = reqMbrLst.toArray(new RequestMatcher[]{ });
    RequestMatcher[] reqWeb = reqWebLst.toArray(new RequestMatcher[]{ });
    log.debug("PUBLIC-ALLOWED:{}{}", "", reqPub);
    log.trace("AUTH-REQUIRE:{}{}", "", reqMbr);
    log.trace("WEB-RESOURCE:{}{}", "", reqWeb);
    
    http
      /* Cross-Site Request Forgery */ 
      .csrf(csrf -> csrf.disable())
      /* Filter */
      .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
      // .addFilterBefore((req, res, chain) -> {
      //   chain.doFilter(req, res);
      // }, UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(exh -> exh
        /* Throw error when Not Authorized */
        .authenticationEntryPoint(authPoint)
        /* Throw error when Authorization Failed */
        .accessDeniedHandler(authHandler)
      )
      .headers(hdr ->
        hdr.frameOptions(frm -> frm.sameOrigin())
          /* Same Site Referer Only */
          .referrerPolicy(ref -> ref.policy(ReferrerPolicy.SAME_ORIGIN))
          /* xss protection */
          .xssProtection(xss -> xss.disable())
      )
      /* Disable session (using token) */
      .sessionManagement(mng -> mng
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
      /* Check Auth per URI */
      .authorizeHttpRequests(req -> req
        /* Allow for All */
        .requestMatchers(reqPub).permitAll()
        /* Allow for Authroized User */
        .requestMatchers(reqMbr).hasAnyAuthority("ROLE_USER")
        /* Web Resource */
        .requestMatchers(reqWeb).permitAll()
        .anyRequest()
          // .permitAll()
          .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
      )
      /* Disable Form login */
      .formLogin(login -> login.disable())
      /* Disable Form logout */
      .logout(logout -> logout.disable())
      /* Disable Anonymouse user */
      .anonymous(anon -> anon.disable())
      ;
    
    SecurityFilterChain ret = http.build();
    return ret;
  }

  @Component public static class AuthFilter extends GenericFilterBean {
    @Override public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain chain)
      throws IOException, ServletException {
      chain.doFilter(sreq, sres);
    }
  }

  /* Throw error when Not Authorized */
  @Component public static class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override public void commence(HttpServletRequest req, HttpServletResponse res,
      AuthenticationException e) throws IOException, ServletException {
      log.debug("AUTH-ERR:{} {}", req.getRequestURI(), e.getMessage());
      res.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.name());
    }
  }

  /* Throw error when Authorization Failed */
  @Component public static class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override public void handle(HttpServletRequest req, HttpServletResponse res,
      AccessDeniedException e) throws IOException, ServletException {
      log.debug("ACCESS-DENIED:{}", e.getMessage());
      res.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.name());
    }
  }

  /* Cross Origin Resource Sharing Filter Config   */
  @Configuration public static class CorsFilterConfig {
    @Bean CorsFilter corsFilter() {
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      CorsConfiguration cfg = new CorsConfiguration();
      cfg.setAllowCredentials(true);
      cfg.addAllowedOriginPattern("*");
      cfg.addAllowedHeader("*");
      cfg.addAllowedMethod("*");
      source.registerCorsConfiguration("/api/**", cfg);
      return new CorsFilter(source);
    }
  }
}
