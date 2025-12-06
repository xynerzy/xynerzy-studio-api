/**
 * @File        : CoreSettings.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Core system utility
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.system.runtime;

import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.DataUtil.getCascade;
import static com.xynerzy.commons.DataUtil.mergeMap;
import static com.xynerzy.commons.DataUtil.newMap;
import static com.xynerzy.commons.IOUtil.getFile;
import static com.xynerzy.commons.IOUtil.getReader;
import static com.xynerzy.commons.IOUtil.openResourceStream;
import static com.xynerzy.commons.IOUtil.safeclose;
import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.ReflectionUtil.copyOf;
import static com.xynerzy.commons.ReflectionUtil.parsePrimeType;
import static com.xynerzy.commons.StringUtil.concat;
import static com.xynerzy.commons.WebUtil.currentRequest;
import static com.xynerzy.commons.WebUtil.currentResponse;
import static com.xynerzy.commons.WebUtil.currentSession;
import static java.lang.Math.ceil;
import static java.lang.Thread.currentThread;
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.yaml.snakeyaml.Yaml;

import com.xynerzy.Application;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Component
public class CoreSystem implements ApplicationContextAware, ServletContextListener {

  private static CoreSystem instance = null;
  
  private static final Pattern PTN_PLACEHOLDER = Pattern.compile("[$][{]([a-zA-Z0-9_.-]+)([:].*){0,1}[}]");

  private BlockingQueue<Runnable> threadQueue;

  private ThreadPoolExecutor executor;

  private Map<String, Object> globalContext;

  @Autowired @Getter private Environment environment;

  @Getter private ApplicationContext appctx;

  @Getter private ServletContext svctx;

  @Getter private Settings settings;

  @Getter private File staticWeb;

  private CoreSystem() {
    log.trace("INSTANCE:{}", instance);
    synchronized(CoreSystem.class) {
      if (instance == null || instance != this) {
        if (instance != null && instance.threadQueue != null) { this.threadQueue = instance.threadQueue; }
        if (instance != null && instance.executor != null) { this.executor = instance.executor; }
        if (instance != null && instance.settings != null) { this.settings = instance.settings; }
        if (instance != null && instance.globalContext != null) { this.globalContext = instance.globalContext; }
        instance = this;
        initCore();
        initEnvr();
        log.info("INSTANCE:{}", instance);
      }
    }
  }

  public static CoreSystem getInstance() {
    synchronized(CoreSystem.class) {
      if (instance == null) {
        instance = new CoreSystem();
      }
      return instance;
    }
  }

  private void initCore() {
    log.info("INIT-CORE...");
    try {
      if (this.executor == null) {
        threadQueue = new LinkedBlockingQueue<>();
        executor = new ThreadPoolExecutor(2, 4, 1000 * 60 * 1, MILLISECONDS, threadQueue);
        globalContext = synchronizedMap(new LinkedHashMap<>());
      }
      if (this.settings == null) { this.settings = new Settings(); }
      reloadSettings();
      int threadMaximumPoolSize = settings.getCoreThreadMaximumPoolSize();
      long threadKeepAliveTime = settings.getCoreThreadKeepAliveTime();
      if (threadMaximumPoolSize < 1) { threadMaximumPoolSize = 1; }
      if (threadMaximumPoolSize > 32) { threadMaximumPoolSize = 32; }
      if (threadKeepAliveTime < 1000) { threadKeepAliveTime = 1000; }
      if (threadKeepAliveTime > 1000 * 60 * 60 * 24) { threadKeepAliveTime = 1000 * 60 * 60 * 24; }
      executor.setMaximumPoolSize(threadMaximumPoolSize);
      executor.setCorePoolSize((int) ceil(threadMaximumPoolSize / 2));
      executor.setKeepAliveTime(threadKeepAliveTime, MILLISECONDS);
    } finally {
    }
  }

  private void initEnvr() {
    try {
      if (environment != null) {
        {
          org.springframework.web.context.support.StandardServletEnvironment envr = cast(environment, envr = null);
          java.util.Iterator<org.springframework.core.env.PropertySource<?>> iter = envr.getPropertySources().iterator();
          while(iter.hasNext()) {
            org.springframework.core.env.PropertySource<?> itm = iter.next();
            if (itm instanceof org.springframework.boot.env.OriginTrackedMapPropertySource) {
              org.springframework.boot.env.OriginTrackedMapPropertySource prop = cast(itm, prop = null);
              log.trace("CHECK:{} / {}", itm.getName(), Arrays.asList(prop.getPropertyNames()));
            }
          }
        }
        log.trace("ENVIRONMENT:{}", environment.getClass());
        String webpath = environment.getProperty("spring.web.resources.static-locations");
        log.info("WEB-PATH:{}", webpath);
        if (webpath != null) {
          if (webpath.startsWith("classpath:/")) {
            /* {war-file-pth}/!WEB-INF/classes/!/{webpath} */
            staticWeb = getFile(Application.class.getClassLoader()
              .getResource(webpath
                .replaceAll("^classpath:/", "")
                .replaceAll("/$", "")));
          } else {
            staticWeb = getFile(webpath);
          }
          /**
           * GRADLE BOOT-RUN: xynerzy-studio-java/build/resources/main/static
           * WAR STANDALONE : nested:xynerzy-studio-java-0.0.1-SNAPSHOT.war/!WEB-INF/classes/!/static
           * TOMCAT DEPLOY : /tomcat/webapps/xynerzy-studio/WEB-INF/classes/static
           **/
          log.info("STATIC-WEB:{}", staticWeb);
        }
      }
    } finally {
    }
  }

  @Override public void setApplicationContext(@NonNull ApplicationContext appctx) throws BeansException {
    this.appctx = appctx;
    log.info("setApplicationContext");
    WebApplicationContext webctx = (WebApplicationContext) appctx;
    {
      Class<?> bcls = null;
      Object bean = null;
      LOOP1: for (String name : appctx.getBeanDefinitionNames()) {
        if (name == null) { continue LOOP1; }
        if ((bcls = appctx.getType(name)) == null) { continue; }
        if (bcls == CoreSystem.class) { continue; }
        log.trace("BEAN:{}", bcls.getName());
        bean = appctx.getBean(name);
        try {
          Method[] methods = bcls.getMethods();
          LOOP2: for (Method m : methods) {
            if (m == null) { continue LOOP2; }
            String mname = m.getName();
            if (
              bean != null &&
              "inject".equals(mname) &&
              m.getParameterCount() == 1 &&
              m.getParameterTypes()[0].equals(ApplicationContext.class)) {
              m.invoke(bean, new Object[] { appctx });
            }
          }
          // Method m = bcls.getDeclaredMethod("inject", new Class[] { ApplicationContext.class });
        } catch (Exception e) {
          log.debug("E:{}", e.getMessage());
        }
      }
    }
    {
      try {
        svctx = webctx.getServletContext();
        String svrInfo = svctx.getServerInfo();
        log.debug("SERVLET-CONTEXT:{} / {}", svctx, svrInfo);
        if (svrInfo.startsWith("Apache Tomcat")) {
          /* tomcat / boot / stand-alone */
        } else {
        }
      } catch (Exception e) {
        log.debug("E:{}", e.getMessage());
      }
    }
    {
      RequestMappingHandlerAdapter bean = appctx.getBean(RequestMappingHandlerAdapter.class);
      List<HttpMessageConverter<?>> list = bean.getMessageConverters();
      for (HttpMessageConverter<?> itm : list) {
        try {
          log.trace("Converter:{}", itm);
        } catch (Exception e) {
          log.debug("E:{}", e.getMessage());
        }
      }
    }
    {
      Settings bean = appctx.getBean(Settings.class);
      if (bean != null && settings != null) {
        this.settings = copyOf(settings, bean);
      }
    }
    initEnvr();
  }

  @Override public void contextInitialized(ServletContextEvent evt) {
    log.info("contextInitialized");
    svctx = evt.getServletContext();
  }
  @Override public void contextDestroyed(ServletContextEvent sce) {
    log.info("destroy..");
  }
  
  public void reloadSettings() {
    log.debug("RELOADING-CONFIG...");
    Yaml yaml = new Yaml();
    Reader reader = null;
    Map<String, Object> settingMap = newMap();
    String profile = "";
    
    profile = System.getProperty("spring.profiles.active");
    
    /** 1. read yml properties */
    profile = concat(profile).split(" ")[0];
    log.debug("PROFILE:{}", profile);
    String osname = concat(System.getProperty("os.name")).toLowerCase(Locale.getDefault());
    String timezone = System.getProperty("user.timezone");
    String encoding = System.getProperty("file.encoding");
    log.info("SYSTEM:{} / {} / {} / {}", osname, timezone, encoding, profile);
    String fileName = "";
    for (int inx = 0; inx < 2; inx++) {
      try {
        switch (inx) {
        case 0: {
          fileName = concat("/application.yml");
        } break;
        default: 
          fileName = concat("/application-", profile, ".yml");
        }
        log.debug("SETTINGS-FILE:{}", fileName);
        reader = getReader(openResourceStream(Application.class, fileName), UTF8);
        settingMap = mergeMap(settingMap, yaml.load(reader));
      } catch (Exception e) {
        log.debug("APPLICATION-PROPERTY PROFILE {} NOT FOUND", profile);
      } finally {
        safeclose(reader);
      }
    }
    if (log.isTraceEnabled()) { log.trace("SETTINGS:{} / {}", profile, new JSONObject(settingMap).toString(2)); }
    {
      String overridePath = cast(getCascade(settingMap, "system.settings.override".split("[.]")), "");
      log.debug("SETTINGS-OVERRIDE:{}", overridePath);
    }
    /** 2. map to settings from yml data */
    String nam = "";
    String def = "";
    Settings settings = this.getSettings();
    for (Field field: Settings.class.getDeclaredFields()) {
      try {
        if (field == null) { continue; }
        Value anon = field.getAnnotation(Value.class);
        if (anon == null) { continue; }
        String aname = anon.value();
        Object val = null;
        Matcher fmat = PTN_PLACEHOLDER.matcher(aname);
        // log.debug("KEY:{} / {}", ak, mat);
        if (fmat.find() && fmat.groupCount() > 0) {
          Class<?> type = field.getType();
          nam = fmat.group(1);
          if (fmat.groupCount() > 1) { def = concat(fmat.group(2)).replaceAll("^[:]", ""); }
          val = getCascade(settingMap, nam.split("[.]"));
          if (val == null) { val = def; }
          log.trace("KEY:{} / {} / {} / {} / {}", nam, val, def, type, field);
          val = parsePrimeType(type, val);
          if (type == String.class) {
            val = fillPlaceholder(String.valueOf(val), nam);
          }
          if (settings != null && val != null) {
            log.trace("SET-VALUE:{} = {} / {}", field, val, settings);
            field.setAccessible(true);
            field.set(settings, val);
          }
        }
      } catch (Exception e) {
        log.debug("E:{}", e.getMessage());
      }
    }
  }

  private static String fillPlaceholder(String val, String nam) {
    String str = val;
    String v = null;
    Matcher vmat = PTN_PLACEHOLDER.matcher(str);
    if (vmat.find()) {
      String k = vmat.group(1);
      LOOP: for (int kinx = 0; kinx < 10; kinx++) {
        SW: switch (kinx) {
        case 0: {
          v = System.getProperty(k);
        } break SW;
        case 1: {
          v = System.getenv(k);
        } break SW;
        default: v = null;
        }
        if (v != null && !"".equals(v)) { break LOOP; }
      }
      if (v != null && !"".equals(v)) {
        str = concat(str.substring(0, vmat.start()), v, str.substring(vmat.end()));
        log.trace("VALUE:{} = {} / {} = {}", nam, str, k, v);
        val = str;
      }
    }
    return val;
  }

  public static void putGlobal(String k, Object v) { instance.globalContext.put(k, v); }
  public static Object getGlobal(String k) { return instance.globalContext.get(k); }
  public static <T> T getGlobal(String k, T v) { return cast(instance.globalContext.get(k), v); }
  public static Object removeGlobal(String k) { return instance.globalContext.remove(k); }
  public static <T> T removeGlobal(String k, T v) { return cast(instance.globalContext.remove(k), v); }
  public static void executeBackground(Runnable proc) { executeBackground(proc, null, null, null); }
  public static void executeBackground(Runnable proc, HttpServletRequest req, HttpServletResponse res, HttpSession ss) {
    while(instance.threadQueue.size() > instance.executor.getMaximumPoolSize()) {
      try {
        synchronized(instance.threadQueue) { instance.threadQueue.wait(100); }
      } catch (Exception e) { log.trace("E:{}", e.getMessage()); }
    }
    instance.executor.execute(() -> {
      if (req != null) { putGlobal(concat(HttpServletRequest.class.getName(), "@", currentThread().getId()), req != null ? req : currentRequest()); }
      if (res != null) { putGlobal(concat(HttpServletResponse.class.getName(), "@", currentThread().getId()), res != null ? res : currentResponse()); }
      if (ss != null) { putGlobal(concat(HttpSession.class.getName(), "@", currentThread().getId()), ss != null ? ss : currentSession()); }
      try {
        proc.run();
      } catch (Exception e) {
        log.info("E:", e);
      }
      if (req != null) { removeGlobal(concat(HttpServletRequest.class.getName(), "@", currentThread().getId())); }
      if (res != null) { removeGlobal(concat(HttpServletResponse.class.getName(), "@", currentThread().getId())); }
      if (ss != null) { removeGlobal(concat(HttpSession.class.getName(), "@", currentThread().getId())); }
      try {
        synchronized(instance.threadQueue) { instance.threadQueue.notify(); }
      } catch (Exception e) { log.trace("E:{}", e.getMessage()); }
    });
  }

  @Component public static class ApplicationEventListener implements ApplicationListener<ApplicationEvent> {
    @Override public void onApplicationEvent(@NonNull ApplicationEvent evt) {
      log.trace("APPLICATION-EVENT:{}", evt.getClass().getName());
      switch (evt.getClass().getName()) {
      case "org.springframework.messaging.simp.broker.BrokerAvailabilityEvent": {
      } break;
      case "org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent": {
      } break;
      case "org.springframework.context.event.ContextRefreshedEvent": {
      } break;
      case "org.springframework.boot.context.event.ApplicationStartedEvent": {
      } break;
      case "org.springframework.boot.availability.AvailabilityChangeEvent": {
      } break;
      case "org.springframework.boot.context.event.ApplicationReadyEvent": {
      } break;
      case "org.springframework.web.context.support.ServletRequestHandledEvent": {
      } break;
      default: }
    }
  }

  @Component public static class WebServerInitListener implements ApplicationListener<WebServerInitializedEvent> {
    @Override public void onApplicationEvent(@NonNull WebServerInitializedEvent evt) {
      WebServer wsvr = evt.getWebServer();
      log.debug("WEB-SERVER-EVT:{}", wsvr);
      if (wsvr != null) {
        if (wsvr.getClass().getName().indexOf("boot.web.embedded.tomcat.TomcatWebServer") != -1) {
          log.debug("RUNNING ON TOMCAT-EMBEDED PORT:{}", wsvr.getPort());
        }
      }
    }
  }

  @Component public static class SessionEventListener implements HttpSessionListener, HttpSessionAttributeListener {
    @Override public void sessionCreated(HttpSessionEvent evt) {
      log.trace("SESSION_CREATED:{}", evt);
    }
    @Override public void sessionDestroyed(HttpSessionEvent evt) {
      log.trace("SESSION_DESTROYED:{}", evt);
    }
    @Override public void attributeAdded(HttpSessionBindingEvent evt) {
      log.trace("SESSION_ATTRIBUTE_ADDED:{}", evt);
    }
    @Override public void attributeRemoved(HttpSessionBindingEvent evt) {
      log.trace("SESSION_ATTRIBUTE_REMOVED:{}", evt);
    }
    @Override public void attributeReplaced(HttpSessionBindingEvent evt) {
      log.trace("SESSION_ATTRIBUTE_REPLACED:{}", evt);
    }
  }

  @Component public static class CoreSystemFilter extends HttpFilter {
    @Override public void doFilter(HttpServletRequest sreq, HttpServletResponse sres, FilterChain chain) throws IOException, ServletException {
      chain.doFilter(sreq, sres);
    }
  }

  @Component public static class CoreSystemWebRequestInterceptor extends WebRequestHandlerInterceptorAdapter {
    public CoreSystemWebRequestInterceptor(WebRequestInterceptor intr) {
      super(intr);
    }
    
    @Override public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) throws Exception {
      return true;
    }

    @Override public void postHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable ModelAndView mav) throws Exception {
    }
  }
}