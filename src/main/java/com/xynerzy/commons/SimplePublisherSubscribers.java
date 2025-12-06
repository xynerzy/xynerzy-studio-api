/**
 * @File        : SimpleMessageBroker.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-31
 * @Description : Simple Message Broker
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.ReflectionUtil.cast;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimplePublisherSubscribers<E> implements Closeable {
  private final List<TopicEntry<E>> publisher = synchronizedList(new ArrayList<>());
  private final Map<String, Object> productStore = synchronizedMap(new LinkedHashMap<>());
  private final Map<Long, TopicEntry<BlockingQueue<E>>> subscribers = synchronizedMap(new LinkedHashMap<>());
  private final Object lock = new Object();
  private final AtomicBoolean alive = new AtomicBoolean(false);
  private final BlockingQueue<Runnable> executeQueue = new LinkedBlockingQueue<>();
  private final Executor executor = new ThreadPoolExecutor(1, 1, 10, TimeUnit.MINUTES, executeQueue);
  private final AtomicLong seq = new AtomicLong(0);
  private int maxRetry = 4;
  private long maxLiveInterval = 3000;
  private long thresholdDelay = 400;

  public SimplePublisherSubscribers() { this(-1, -1, -1); }
  public SimplePublisherSubscribers(int maxRetry) { this(maxRetry, -1, -1); }
  public SimplePublisherSubscribers(int maxRetry, long thresholdDelay) { this(maxRetry, thresholdDelay, -1); }
  public SimplePublisherSubscribers(int maxRetry, long thresholdDelay, long maxLiveInterval) {
    if (maxRetry > 0) { this.maxRetry = maxRetry; }
    if (maxLiveInterval > 0) { this.maxLiveInterval = maxLiveInterval; }
    if (thresholdDelay > 0) { this.thresholdDelay = thresholdDelay; }
  }

  public void setAlive(boolean alive) {
    this.alive.set(alive);
    synchronized(lock) { lock.notify(); }
  }

  public boolean isAlive() {
    try {
      synchronized (lock) {
        lock.wait(1);
        lock.notify();
      } } catch (InterruptedException e) {
      return alive.get();
    }
    return alive.get();
  }

  public void join(long timeout) {
    try {
      do {
        synchronized (lock) {
          if (timeout > 0) { lock.wait(timeout); } else { lock.wait(); }
          lock.notify();
        }
      } while(alive.get());
    } catch (Exception e) {
      log.debug("E:", e);
      return;
    }
  }

  public void publish(String uid, String topic, E itm) {
    synchronized (this) { sleep(1); }
    if (topic == null || "".equals(topic) || itm == null) { return; }
    long ctime = System.currentTimeMillis();
    if (!alive.get()) {
      alive.set(true);
      if (!executeQueue.contains(agent)) { executor.execute(agent); }
    }
    log.debug("PUSH:{} / {} / {} / {}", topic, itm, publisher.size(), subscribers.size());
    TopicEntry<E> product = new TopicEntry<>(uid, topic, itm, ctime);
    if (uid != null && !"".equals(uid)) { productStore.put(uid, product); }
    publisher.add(product);
    synchronized(lock) { lock.notify(); }
    synchronized(this) { sleep(1); }
  }

  public E remove(String uid) {
    E ret = null;
    int sizePublisher = 0;
    int sizeStore = 0;
    synchronized (publisher) {
      TopicEntry<E> matcher = cast(productStore.remove(uid), matcher = null);
      if (matcher == null) { return ret; }
      for (int inx = 0; inx < publisher.size(); inx++) {
        if (matcher.equals(publisher.get(inx))) {
          publisher.set(inx, null);
          ret = matcher.getData();
        }
      }
      sizePublisher = publisher.size();
      sizeStore = productStore.size();
      if (log.isDebugEnabled()) {
        log.debug("AFTER-REMOVE:{} / {}", sizePublisher, sizeStore);
        if (sizePublisher > 0) { log.debug("PUBLISHER:{}", publisher); }
        if (sizeStore > 0) { log.debug("STORE:{}", productStore); }
      }
    }
    return ret;
  }

  public void republish(String topic) {
    try {
      synchronized (lock) {
        lock.wait(1);
        lock.notify();
      }
      if (topic == null || "".equals(topic)) { return; }
      synchronized (publisher) {
        for (int inx = 0; inx < publisher.size(); inx++) {
          TopicEntry<E> product = publisher.get(inx);
          if (topic.equals(product.getTopic()) & product.isProcessed()) {
            product.setProcessed(false);
          }
        }
      }
      synchronized (lock) { lock.notify(); }
    } catch (Exception e) {
      log.trace("E:", e);
    }
  }
  
  public E subscribe(String topic, long maxTime, E def) {
    E ret = null;
    if (topic == null || "".equals(topic)) { return ret; }
    long ctime = System.currentTimeMillis();
    long tid = seq.incrementAndGet();
    if (!alive.get()) {
      alive.set(true);
      if (!executeQueue.contains(agent)) { executor.execute(agent); }
    }
    synchronized (lock) { lock.notify(); }
    try {
      BlockingQueue<E> queue = null;
      synchronized (subscribers) {
        if (!subscribers.containsKey(tid)) {
          subscribers.put(tid, new TopicEntry<>(null, topic, queue = new LinkedBlockingQueue<>(), ctime));
        }
      }
      if (queue != null) {
        if (maxTime > 0) {
          ret = queue.poll(maxTime, MILLISECONDS);
        } else {
          ret = queue.poll();
        }
      }
    } catch (Exception e) {
      ret = null;
    }
    if (ret == null) {
      synchronized (subscribers) { subscribers.remove(tid); }
      log.debug("POLL-RESULT:{} / {}", tid, ret);
    }
    if (ret == null) { ret = def; }
    return ret;
  }

  private final Runnable agent = () -> {
    boolean found = false;
    Set<Long> tids = synchronizedSet(new LinkedHashSet<>());
    TopicEntry<E> product = null;
    log.debug("START-AGENT..");
    LOOP1: for (int retry = 0; retry < maxRetry; retry++) {
      try {
        if (publisher.size() == 0) {
          synchronized (lock) {
            lock.wait(thresholdDelay);
            lock.notify();
          }
        }
        synchronized (publisher) {
          LOOP2: for (int inx = 0; inx < publisher.size(); inx++) {
            // long ctime = currentTimeMillis();
            product = publisher.get(inx);
            if (product == null || product.isProcessed() || product.getTopic() == null) { continue LOOP2; }
            String topic = product.getTopic();
            E data = product.getData();
            found = false;
            synchronized (subscribers) { tids.addAll(subscribers.keySet()); }
            LOOP3: for (Long tid : tids) {
              TopicEntry<BlockingQueue<E>> subscriber = null;
              subscriber = subscribers.get(tid);
              if (subscriber == null) { continue LOOP3; }
              if (topic != null && topic.equals(subscriber.getTopic())) {
                subscribers.put(tid, null);
                BlockingQueue<E> queue = subscriber.getData();
                if (queue != null) { queue.add(data); }
                found = true;
              }
              if (!alive.get()) { break LOOP3; }
            }
            if (found && publisher.size() > inx) {
              publisher.set(inx, null);
              inx--;
              continue LOOP2;
            }
            if (!alive.get()) { break LOOP2; }
          }
        }
        LOOP4: for (Long tid : tids) {
          TopicEntry<BlockingQueue<E>> subscriber = null;
          synchronized (subscribers) {
            subscriber = subscribers.get(tid);
            if (subscriber == null) {
              subscribers.remove(tid);
              continue LOOP4;
            }
          }
          if (!alive.get()) { break LOOP4; }
        }
        synchronized (publisher) {
          TopicEntry<E> itm = null;
          long ctime = System.currentTimeMillis();
          // log.debug("PUBLISHER-SIZE:{}", publisher.size());
          for (int inx = 0; inx < publisher.size(); inx++) {
            // log.debug("CHECK:{}", publisher.get(inx));
            if ((product = publisher.get(inx)) != null) {
              if (product.getCtime() + maxLiveInterval <= ctime) {
                itm = publisher.remove(inx);
                if (itm != null && itm.getUid() != null) { productStore.remove(itm.getUid()); }
                inx--;
              }
            } else {
              itm = publisher.remove(inx);
              if (itm != null && itm.getUid() != null) { productStore.remove(itm.getUid()); }
              inx--;
            }
          }
        }
        synchronized (subscribers) {
          if (subscribers.size() > 0 && !found) { sleep(1); }
        }
        if (publisher.size() == 0 && subscribers.size() == 0) {
          if (retry >= (maxRetry - 1)) { alive.set(false); }
        } else {
          retry = 0;
          alive.set(true);
        }
      } catch (InterruptedException e) {
        log.debug("E:", e);
        alive.set(false);
      }
      if (!alive.get()) { break LOOP1; }
    }
    log.debug("FINISH-AGENT..");
    synchronized (lock) { lock.notify(); }
  };

  @Override public void close() {
    log.trace("close");
    clear();
  }

  public void clear() {
    alive.set(false);
    publisher.clear();
    productStore.clear();
    subscribers.clear();
  }

  public void dumpState() {
    log.debug("PUBSUB-IS-ALIVE:{}", isAlive());
    log.debug("PUBSUB-PRODUCERS:{} / {}",
      publisher.size(),
      StreamSupport.stream(publisher.spliterator(), false)
      .map(v -> v.getTopic())
      .collect(Collectors.toSet())
    );
    log.debug("PUBSUB-SUBS:{} / {}",
      subscribers.size(),
      StreamSupport.stream(subscribers.values().spliterator(), false)
      .map(v -> v.getTopic())
      .collect(Collectors.toSet())
    );
    log.debug("PUBSUB-PRODSTORE:{} / {}",
      productStore.size(),
      productStore.keySet());
  }

  public static void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (Exception e) {
      return;
    }
  }

  @Getter @Setter
  public static class TopicEntry<E> {
    private String uid;
    private String topic;
    private E data;
    boolean processed;
    private long ctime;

    public TopicEntry(String uid, String topic, E data, long ctime) {
      this.uid = uid;
      this.topic = topic;
      this.data = data;
      this.ctime = ctime;
      this.processed = false;
    }
  }
}
