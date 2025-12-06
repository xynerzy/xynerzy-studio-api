/**
 * @File        : IOUtil.java
 * @Author      : lupfeliz@gmail.com
 * @Since       : 2025-10-08
 * @Description : Java Input / Output utility
 * @Site        : https://github.com/lupfeliz/xynerzy-studio-java
 **/
package com.xynerzy.commons;

import static com.xynerzy.commons.Constants.UTF8;
import static com.xynerzy.commons.ReflectionUtil.cast;
import static com.xynerzy.commons.StringUtil.concat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOUtil {

  public static final int passthrough(Object in, Object out) throws IOException { return passthrough(in, out, UTF8, 4096); }
  public static final int passthrough(Object in, Object out, String charset, int bufsize) throws IOException {
    int ret = 0;
    if (in == null || out == null) { return ret; }
    if (in instanceof InputStream) {
      InputStream istream = cast(in, istream = null);
      if (out instanceof OutputStream) {
        OutputStream ostream = cast(out, ostream = null);
        byte[] buf = new byte[bufsize];
        for (int rl; (rl = istream.read(buf, 0, buf.length)) != -1;) {
          ret += rl;
          ostream.write(buf, 0, rl);
        }
      } else if (out instanceof Writer) {
        Reader reader = null;
        try { ret = passthrough(reader = getReader(istream, charset), out, charset, bufsize); } finally { safeclose(reader); }
      } else if (out instanceof File) {
        OutputStream ostream = null;
        try { ret = passthrough(in, ostream = getOutputStream(getFile(out)), charset, bufsize); } finally { safeclose(ostream); }
      }
    } else if (in instanceof Reader) {
      Reader reader = cast(in, reader = null);
      if (out instanceof OutputStream) {
        OutputStream ostream = cast(out, ostream = null);
        BufferedWriter writer = getWriter(ostream, charset);
        try { ret = passthrough(in, writer, charset, bufsize); } finally { safeclose(writer); }
      } else if (out instanceof Writer) {
        Writer writer = cast(out, writer = null);
        char[] buf = new char[bufsize];
        for (int rl; (rl = reader.read(buf, 0, buf.length)) != -1;) {
          ret += rl;
          writer.write(buf, 0, rl);
        }
      } else if (out instanceof File) {
        Writer writer = null;
        try { ret = passthrough(in, writer = getWriter(getFile(out), charset), charset, bufsize); } finally { safeclose(writer); }
      }
    } else if (in instanceof File) {
      File file = getFile(in);
      if (out instanceof OutputStream) {
        InputStream istream = null;
        try { ret = passthrough(istream = getInputStream(file), out, charset, bufsize); } finally { safeclose(istream); }
      } else if (out instanceof Writer) {
        Reader reader = null;
        try { ret = passthrough(reader = getReader(file, charset), out, charset, bufsize); } finally { safeclose(reader); }
      } else if (out instanceof File) {
        InputStream istream = null;
        OutputStream ostream = null;
        try { ret = passthrough(istream = getInputStream(file), ostream = getOutputStream(getFile(out)), charset, bufsize); } finally { safeclose(istream); safeclose(ostream); }
      }
    }
    return ret;
  }

  public static final boolean mkdirs(File file) {
    try {
      if (!file.exists()) {
        mkdirs(file.getParentFile());
        file.mkdir();
      }
    } catch (Exception e) {
      // log.error("E:", e);
      return false;
    }
    return true;
  }

  public static boolean deleteFile(File file) {
    boolean ret = false;
    try {
      ret = file.delete();
    } catch (Exception ignore) { log.trace("E:{}", ignore); }
    return ret;
  }

  public static boolean deleteAll(File file) {
    boolean ret = false;
    try {
      if (!file.isDirectory()) {
        ret = file.delete();
      } else {
        file.listFiles(new FileRemoveFilter());
        ret = file.delete();
      }
    } catch (Exception ignore) { log.trace("E:{}", ignore); }
    return ret;
  }

  private static class FileRemoveFilter implements FileFilter {
    @Override public boolean accept(File file) {
      boolean ret = false;
      if (file == null) { return ret; }
      if (!file.exists()) { return ret; }
      if (!file.isDirectory()) {
        file.delete();
      } else {
        file.listFiles(this);
        file.delete();
      }
      return ret;
    }
  }

  public static File getFile(Object base, String... args) {
    File ret = null;
    if (base != null) {
      if (base instanceof File) {
        ret = cast(base, ret);
      } else if (base instanceof URL) {
        ret = new File(((URL) base).getFile());
      } else {
        ret = new File(String.valueOf(base));
      }
    }
    if (args.length > 0) {
      for (String arg : args) {
        if (ret == null) {
            ret = new File(arg);
        } else {
          ret = new File(ret, arg);
        }
      }
    }
    return ret;
  }

  public static InputStream getInputStream(File file) throws IOException {
    InputStream ret = null;
    if (file != null && file.exists()) { ret = new FileInputStream(file); }
    return ret;
  }

  public static BufferedReader getReader(File file, String charset) throws IOException {
    BufferedReader ret = null;
    if (charset == null) { charset = UTF8; }
    if (file != null && file.exists()) { ret = BufferedReaderWrapper.createReader(file, charset); }
    return ret;
  }

  public static BufferedReader getReader(Reader reader) throws IOException {
    BufferedReader ret = null;
    if (reader != null) { ret = BufferedReaderWrapper.createReader(reader); }
    return ret;
  }

  public static BufferedReader getReader(InputStream istream, String charset) throws IOException {
    BufferedReader ret = null;
    if (istream != null) { ret = BufferedReaderWrapper.createReader(istream, charset); }
    return ret;
  }

  public static OutputStream getOutputStream(File file) throws IOException {
    OutputStream ret = null;
    if (file != null) { ret = new FileOutputStream(file); }
    return ret;
  }

  public static BufferedWriter getWriter(File file, String charset) throws IOException {
    BufferedWriter ret = null;
    if (file != null) { ret = BufferedWriterWrapper.createWriter(file, charset); }
    return ret;
  }

  public static BufferedWriter getWriter(OutputStream ostream, String charset) throws IOException {
    BufferedWriter ret = null;
    if (ostream != null) { ret = BufferedWriterWrapper.createWriter(ostream, charset); }
    return ret;
  }

  public static String readAsString(File input) throws IOException { return doReadAsString(input, UTF8); }
  public static String readAsString(File input, String charset) throws IOException { return doReadAsString(input, charset); }
  public static String readAsString(InputStream input) throws IOException { return doReadAsString(input, UTF8); }
  public static String readAsString(InputStream input, String charset) throws IOException { return doReadAsString(input, charset); }
  public static String readAsString(Reader input) throws IOException { return doReadAsString(input, null); }

  private static String doReadAsString(Object input, String charset) throws IOException {
    StringBuilder ret = new StringBuilder();
    BufferedReader reader = null;
    if (input == null) { return null; }
    try {
      if (input instanceof File) {
        reader = getReader((File) input, charset);
      } else if (input instanceof InputStream) {
        reader = getReader((InputStream) input, charset);
      } else if (input instanceof Reader) {
        reader = getReader((Reader) input);
      }
      if (reader != null) {
        for (String rl; (rl = reader.readLine()) != null;) {
          ret.append(rl).append("\n");
        }
      }
    } finally {
      safeclose(reader);
    }
    if (ret.length() > 0) { return ret.substring(0, ret.length() - 1); }
    return String.valueOf(ret);
  }

  public static void writeToFile(String str, File file, String charset) throws IOException {
    Writer writer = null;
    try {
      writer = getWriter(file, charset);
      if (writer != null) { writer.append(str).flush(); }
    } catch (Exception ignore) {
    } finally {
      safeclose(writer);
    }
  }

  public static void safeclose(Closeable o) {
    if (o != null) {
      try {
        o.close();
      } catch (Exception ignore) { log.trace("E:{}", ignore); }
    }
  }

  public static void safeclose(AutoCloseable o) {
    if (o != null) {
      try {
        o.close();
      } catch (Exception ignore) { log.trace("E:{}", ignore); }
    }
  }

  public static class BufferedReaderWrapper extends BufferedReader {
    private List<Closeable> closeables = new ArrayList<>();
    public BufferedReaderWrapper(Reader in) { super(in); }
    public static BufferedReaderWrapper createReader(File file, String charset) {
      BufferedReaderWrapper inst = null;
      InputStream istream = null;
      try {
        if (file != null && file.exists()) {
          istream = new FileInputStream(file);
          inst = createReader(istream, charset);
        }
      } catch (Exception e) {
        safeclose(istream);
      }
      return inst;
    }
    public static BufferedReaderWrapper createReader(InputStream istream, String charset) {
      BufferedReaderWrapper inst = null;
      Reader reader = null;
      try {
        if (istream != null) {
          reader = new InputStreamReader(istream, charset);
          inst = new BufferedReaderWrapper(reader);
          inst.reader = new BufferedReader(reader);
          inst.closeables.add(reader);
          inst.closeables.add(istream);
        }
      } catch (Exception e) {
        safeclose(istream);
        safeclose(reader);
        safeclose(inst);
      }
      return inst;
    }
    public static BufferedReaderWrapper createReader(Reader reader) {
      BufferedReaderWrapper inst = null;
      try {
        if (reader != null) {
          inst = new BufferedReaderWrapper(reader);
          inst.reader = new BufferedReader(reader);
          inst.closeables.add(reader);
        }
      } catch (Exception e) {
        safeclose(reader);
      }
      return inst;
    }
    private BufferedReader reader;
    public int hashCode() { return reader.hashCode(); }
    public boolean equals(Object obj) { return reader.equals(obj); }
    public int read(CharBuffer target) throws IOException { return reader.read(target); }
    public int read() throws IOException { return reader.read(); }
    public int read(char[] cbuf) throws IOException { return reader.read(cbuf); }
    public int read(char[] cbuf, int off, int len) throws IOException { return reader.read(cbuf, off, len); }
    public String toString() { return reader.toString(); }
    public String readLine() throws IOException { return reader.readLine(); }
    public long skip(long n) throws IOException { return reader.skip(n); }
    public boolean ready() throws IOException { return reader.ready(); }
    public boolean markSupported() { return reader.markSupported(); }
    public void mark(int readAheadLimit) throws IOException { reader.mark(readAheadLimit); }
    public void reset() throws IOException { reader.reset(); }
    public Stream<String> lines() { return reader.lines(); }
    public long transferTo(Writer out) throws IOException {
      try {
        Method mtd = BufferedReader.class.getDeclaredMethod("transferTo", new Class<?>[] { Writer.class });
        if (mtd != null) { return cast(mtd.invoke(reader, new Object[] { out }), 0L); }
      } catch (Exception e) {
        passthrough(reader, out);
      }
      return 0L;
    }
    public void close() throws IOException {
      reader.close();
      if (closeables != null && closeables.size() > 0) {
        for (Closeable item : closeables) { safeclose(item); }
      }
    }
  }

  public static class BufferedWriterWrapper extends BufferedWriter {
    private List<Closeable> closeables = new ArrayList<>();
    public BufferedWriterWrapper(Writer out) { super(out); }
    public static BufferedWriterWrapper createWriter(File file, String charset) {
      BufferedWriterWrapper inst = null;
      OutputStream ostream = null;
      try {
        ostream = new FileOutputStream(file);
        inst = createWriter(ostream, charset);
      } catch (Exception e) {
        safeclose(ostream);
      }
      return inst;
    }
    public static BufferedWriterWrapper createWriter(OutputStream ostream, String charset) {
      BufferedWriterWrapper inst = null;
      Writer writer = null;
      try {
        writer = new OutputStreamWriter(ostream, charset);
        inst = new BufferedWriterWrapper(writer);
        inst.writer = new BufferedWriter(writer);
        inst.closeables.add(writer);
        inst.closeables.add(ostream);
      } catch (Exception e) {
        safeclose(ostream);
        safeclose(writer);
        safeclose(inst);
      }
      return inst;
    }
    private BufferedWriter writer;

    public int hashCode() { return writer.hashCode(); }
    public boolean equals(Object obj) { return writer.equals(obj); }
    public void write(int c) throws IOException { writer.write(c); }
    public void write(char[] cbuf, int off, int len) throws IOException { writer.write(cbuf, off, len); }
    public void write(char[] cbuf) throws IOException { writer.write(cbuf); }
    public void write(String s, int off, int len) throws IOException { writer.write(s, off, len); }
    public void write(String str) throws IOException { writer.write(str); }
    public void newLine() throws IOException { writer.newLine(); }
    public void flush() throws IOException { writer.flush(); }
    public Writer append(CharSequence csq) throws IOException { return writer.append(csq); }
    public Writer append(CharSequence csq, int start, int end) throws IOException { return writer.append(csq, start, end); }
    public String toString() { return writer.toString(); }
    public Writer append(char c) throws IOException { return writer.append(c); }
    public void close() throws IOException {
      writer.close();
      if (closeables != null && closeables.size() > 0) {
        for (Closeable item : closeables) { safeclose(item); }
      }
    }
  }

  public static InputStream openResourceStream(Class<?> baseClass, String... path) throws Exception {
    InputStream ret = null;
    Object[] args = cast(path, args = null);
    URL resource = baseClass.getResource(concat(args));
    if (resource != null) { ret = resource.openStream(); }
    log.trace("RESOURCE:{} / {}", path, resource);
    return ret;
  }

  public static String getContentFromResourceAsString(Class<?> baseClass, String... path) throws Exception {
    StringBuilder ret = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = getReader(openResourceStream(baseClass, path), UTF8);
      for (String rl; (rl = reader.readLine()) != null; ret.append(rl));
    } finally {
      safeclose(reader);
    }
    return String.valueOf(ret);
  }
  
  public static int writeByte8ToBuffer(byte[] buf, int pos, int val) {
    buf[pos++] = (byte) (val);
    return pos;
  }

  public static int writeShort16ToBuffer(byte[] buf, int pos, int val) {
    buf[pos++] = (byte) (val >>> 8);
    buf[pos++] = (byte) (val);
    return pos;
  }

  public static int writeInt32ToBuffer(byte[] buf, int pos, int val) {
    buf[pos++] = (byte) (val >>> 24);
    buf[pos++] = (byte) (val >>> 16);
    buf[pos++] = (byte) (val >>> 8);
    buf[pos++] = (byte) (val);
    return pos;
  }

  public static int writeLong64ToBuffer(byte[] buf, int pos, long val) {
    buf[pos++] = (byte) (val >>> 56);
    buf[pos++] = (byte) (val >>> 48);
    buf[pos++] = (byte) (val >>> 40);
    buf[pos++] = (byte) (val >>> 32);
    buf[pos++] = (byte) (val >>> 24);
    buf[pos++] = (byte) (val >>> 16);
    buf[pos++] = (byte) (val >>> 8);
    buf[pos++] = (byte) (val);
    return pos;
  }

  public static int writeFloat32ToBuffer(byte[] buf, int pos, float fval) {
    int val = Float.floatToRawIntBits(fval);
    return writeInt32ToBuffer(buf, pos, val);
  }

  public static int writeDouble64ToBuffer(byte[] buf, int pos, double dval) {
    long val = Double.doubleToRawLongBits(dval);
    return writeLong64ToBuffer(buf, pos, val);
  }

  public static byte readByte8FromBuffer(byte[] buf, int[] pos) {
    return (byte) (buf[pos[0]++] & 0xff);
  }

  public static short readShort16FromBuffer(byte[] buf, int[] pos) {
    return (short) (
      ((buf[pos[0]++] & 0xff) << 8) | 
      ((buf[pos[0]++] & 0xff))
    );
  }

  public static int readInt32FromBuffer(byte[] buf, int[] pos) {
    return (int) (
      ((buf[pos[0]++] & 0xff) << 24) | 
      ((buf[pos[0]++] & 0xff) << 16) | 
      ((buf[pos[0]++] & 0xff) <<  8) | 
      ((buf[pos[0]++] & 0xff))
    );
  }

  public static long readLong64FromBuffer(byte[] buf, int[] pos) {
    return (long) (
      ((buf[pos[0]++] & 0xff) << 56) | 
      ((buf[pos[0]++] & 0xff) << 48) | 
      ((buf[pos[0]++] & 0xff) << 40) | 
      ((buf[pos[0]++] & 0xff) << 32) | 
      ((buf[pos[0]++] & 0xff) << 24) | 
      ((buf[pos[0]++] & 0xff) << 16) | 
      ((buf[pos[0]++] & 0xff) <<  8) | 
      ((buf[pos[0]++] & 0xff))
    );
  }

  public static float readFloat32FromBuffer(byte[] buf, int[] pos) {
    return Float.intBitsToFloat(readInt32FromBuffer(buf, pos));
  }

  public static double readDouble64FromBuffer(byte[] buf, int[] pos) {
    return Double.longBitsToDouble(readLong64FromBuffer(buf, pos));
  }

  public static interface Streamable {
    int readFrom(RawBytesInputStream istream) throws IOException;
    int writeTo(RawBytesOutputStream ostream) throws IOException;
  }

  public static class RawBytesOutputStream {
    private byte[] buffer;
    private OutputStream stream;
    public RawBytesOutputStream(OutputStream stream, byte[] buffer) {
      this.stream = stream;
      this.buffer = buffer;
    }
    public int writeByte8(int v) throws IOException {
      int len = IOUtil.writeByte8ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public int writeShort16(int v) throws IOException {
      int len = IOUtil.writeShort16ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public int writeInt32(int v) throws IOException {
      int len = IOUtil.writeInt32ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public int writeLong64(long v) throws IOException {
      int len = IOUtil.writeLong64ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public int writeFloat32(float v) throws IOException {
      int len = IOUtil.writeFloat32ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public int writeDouble64(double v) throws IOException {
      int len = IOUtil.writeDouble64ToBuffer(buffer, 0, v);
      stream.write(buffer, 0, len);
      return len;
    }
    public void flush() throws IOException {
      stream.flush();
    }
  }
  public static class RawBytesInputStream {
    private byte[] buffer;
    private int[] pos;
    private InputStream istream;
    public RawBytesInputStream(InputStream istream, byte[] buffer) {
      this.istream = istream;
      this.buffer = buffer;
      this.pos = new int[] { 0, -1 };
    }

    private int readToBuffer() throws IOException {
      int ret = -1;
      ret = istream.read(buffer, 0, buffer.length);
      pos[0] = 0;
      pos[1] = ret;
      return ret;
    }

    // public byte readByte8() {
    //   return IOUtil.readByte8FromBuffer(buffer, null);      
    // }
  } 
  
//   public static void writeUChar(BytesBuffer buffer, int i) {
//     writeShort(buffer, i);
//   }
// 
//   public static void writeAChar(BytesBuffer buffer, int i) {
//     buffer.write((byte) i);
//   }
// 
//    ublic static void writeVInt(BytesBuffer buffer, int i) {
//     while ((i & ~0x7F) != 0) {
//       buffer.write((byte) ((i & 0x7F) | 0x80));
//       i >>>= 7;
//     }
//     buffer.write((byte) i);
//   }
// 
//   public static void writeVInt(OutputStream os, int i) throws IOException {
//     while ((i & ~0x7F) != 0) {
//       os.write(((i & 0x7F) | 0x80));
//       i >>>= 7;
//     }
//     os.write(i);
//   }
// 
//   // 입력정수의 가변길이를 계산한다.
//   public static int lenVariableByte(int v) {
//     int byteCnt = 1;
//     // 7바이트씩 몇번 shift를 하게되는지 계산하면 된다. 부호비트를 유지하지 않는 logical shift를 이용한다.
//     while ((v >>>= 7) != 0)
//       byteCnt++;
// 
//     return byteCnt;
//   }
// 
//   public static long readLong(byte[] buffer, int pos) {
//     return (((long) readInt(buffer, pos)) << 32) | (readInt(buffer, pos+4) & 0xFFFFFFFFL);
//   }
// 
//   public static int readInt(BytesBuffer buffer) {
//     return ((buffer.readByte() & 0xFF) << 24) | ((buffer.readByte() & 0xFF) << 16) | ((buffer.readByte() & 0xFF) << 8)
//         | (buffer.readByte() & 0xFF);
//   }
// 
//   public static int readInt(byte[] buffer, int pos) {
//     return ((buffer[pos + 0] & 0xFF) << 24) | ((buffer[pos + 1] & 0xFF) << 16) | ((buffer[pos + 2] & 0xFF) << 8) | (buffer[pos + 3] & 0xFF);
//   }
// 
//   public static short readShort(byte[] buffer, int pos) {
//     return (short) (((buffer[pos] & 0xFF) << 8) | (buffer[pos + 1] & 0xFF));
//   }
// 
//   public static int readVInt(BytesBuffer buffer) {
//     byte b = buffer.readByte();
//     if (b >= 0)
//       return b;
//     int i = b & 0x7F;
//     b = buffer.readByte();
//     i |= (b & 0x7F) << 7;
//     if (b >= 0)
//       return i;
//     b = buffer.readByte();
//     i |= (b & 0x7F) << 14;
//     if (b >= 0)
//       return i;
//     b = buffer.readByte();
//     i |= (b & 0x7F) << 21;
//     if (b >= 0)
//       return i;
//     b = buffer.readByte();
//     // Warning: the next ands use 0x0F / 0xF0 - beware copy/paste errors:
//     i |= (b & 0x0F) << 28;
//     if ((b & 0xF0) == 0)
//       return i;
//     return -1;
//   }
// 
//   public static int readVInt(byte[] buffer, int pos) {
//     byte b = buffer[pos++];
//     if (b >= 0)
//       return b;
//     int i = b & 0x7F;
//     b = buffer[pos++];
//     i |= (b & 0x7F) << 7;
//     if (b >= 0)
//       return i;
//     b = buffer[pos++];
//     i |= (b & 0x7F) << 14;
//     if (b >= 0)
//       return i;
//     b = buffer[pos++];
//     i |= (b & 0x7F) << 21;
//     if (b >= 0)
//       return i;
//     b = buffer[pos++];
//     // Warning: the next ands use 0x0F / 0xF0 - beware copy/paste errors:
//     i |= (b & 0x0F) << 28;
//     if ((b & 0xF0) == 0)
//       return i;
//     return -1;
//   }
// 
//   public static char[] readAChars(byte[] buffer, int pos, int len) {
//     char[] array = new char[len];
//     for (int i = 0; i < len; i++)
//       array[i] = (char) buffer[pos + i];
//     return array;
//   }
// 
//   public static char[] readUChars(byte[] buffer, int pos, int len) {
//     char[] array = new char[len / 2];
//     for (int i = 0; i < array.length; i++) {
//       array[i] = (char) readShort(buffer, pos + i * 2);
//     }
//     return array;
//   }
// 
//   public static boolean copy(File a, File b) throws IOException {
//     FileChannel fc1 = new FileInputStream(a).getChannel();
//     if (b.exists())
//       b.delete();
//     FileChannel fc2 = new FileOutputStream(b).getChannel();
//     long count1 = fc1.size();
//     fc1.transferTo(0, count1, fc2);
//     fc1.close();
//     fc2.close();
//     return true;
//   }
//   public static void transferFrom(DataOutput output, File inFile, long offset, long length, byte[] buffer) throws IOException {
//     RandomAccessFile rin = null;
//     try {
//       rin = new RandomAccessFile(inFile, "r");
//       rin.seek(offset);
//       int size = Math.min((int) length, buffer.length);// 대부분 buffer사이즈가 되며, length가 작을 경우에만, length가 된다.
//       while (length > 0) {
//         int nread = rin.read(buffer, 0, size);
//         output.writeBytes(buffer, 0, nread);
//         length -= nread;
//       }
//     } finally {
//       if (rin != null) {
//         rin.close();
//       }
//     }
//   }
//   public static void transferFrom(DataOutput output, DataInput input, long length, byte[] buffer) throws IOException {
//     while (length > 0) {
//       int size = Math.min((int) length, buffer.length);
//       input.readBytes(buffer, 0, size);
//       output.writeBytes(buffer, 0, size);
//       length -= size;
//     }
//   }
//   public static byte readByte(RandomAccessFile raf) throws IOException {
//     return raf.readByte();
//   }
//   public static int readInt(RandomAccessFile raf) throws IOException {
//     return ((readByte(raf) & 0xFF) << 24) | ((readByte(raf) & 0xFF) << 16) | ((readByte(raf) & 0xFF) << 8) | (readByte(raf) & 0xFF);
//   }
//   public static long readLong(RandomAccessFile raf) throws IOException {
//     return (((long) readInt(raf)) << 32) | (readInt(raf) & 0xFFFFFFFFL);
//   }
}
