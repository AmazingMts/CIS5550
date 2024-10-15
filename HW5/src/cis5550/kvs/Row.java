package cis5550.kvs;

import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Row implements Serializable {

  protected String key;
  protected ConcurrentHashMap<String, byte[]> values;
  Map<String, TreeMap<Integer, byte[]>> columns;
  public Row(String keyArg) {
    key = keyArg;
    values = new ConcurrentHashMap<>();
    columns = new TreeMap<>();
  }

  public synchronized String key() {
    return key;
  }

  public Row clone() {
    Row theClone = new Row(key);
    theClone.values = new ConcurrentHashMap<>(this.values);  // 使用构造函数复制
    return theClone;
  }

  public synchronized Set<String> columns() {
    return values.keySet();
  }

  public  void put(String key, String value) {
    values.put(key, value.getBytes());
  }

  public void put(String column, byte[] data) {
    TreeMap<Integer, byte[]> versions = columns.getOrDefault(column, new TreeMap<>());
    int newVersion = versions.isEmpty() ? 1 : versions.lastKey() + 1;
    versions.put(newVersion, data);
    columns.put(column, versions);
  }

  public byte[] get(String column) {
    TreeMap<Integer, byte[]> versions = columns.get(column);

    return (versions != null && !versions.isEmpty()) ? versions.lastEntry().getValue() : null;
  }
  public byte[] getVersion(String column, int version) {
    TreeMap<Integer, byte[]> versions = columns.get(column);
    return (versions != null) ? versions.get(version) : null;
  }

  // 获取该列的最新版本号
  public int getLatestVersion(String column) {
    TreeMap<Integer, byte[]> versions = columns.get(column);
    return (versions != null && !versions.isEmpty()) ? versions.lastKey() : 0;
  }

  public synchronized byte[] getBytes(String key) {
    return values.get(key);
  }

  static String readStringSpace(InputStream in) throws Exception {
    byte buffer[] = new byte[16384];
    int numRead = 0;
    while (true) {
      if (numRead == buffer.length)
        throw new Exception("Format error: Expecting string+space");

      int b = in.read();
      if ((b < 0) || (b == 10))
        return null;
      buffer[numRead++] = (byte)b;
      if (b == ' ')
        return new String(buffer, 0, numRead-1);
    }
  }

  static String readStringSpace(RandomAccessFile in) throws Exception {
    byte buffer[] = new byte[16384];
    int numRead = 0;
    while (true) {
      if (numRead == buffer.length)
        throw new Exception("Format error: Expecting string+space");

      int b = in.read();
      if ((b < 0) || (b == 10))
        return null;
      buffer[numRead++] = (byte)b;
      if (b == ' ')
        return new String(buffer, 0, numRead-1);
    }
  }

  public static Row readFrom(InputStream in) throws Exception {
    String theKey = readStringSpace(in);
    if (theKey == null)
      return null;

    Row newRow = new Row(theKey);
    while (true) {
      String keyOrMarker = readStringSpace(in);
      if (keyOrMarker == null)
        return newRow;

      int len = Integer.parseInt(readStringSpace(in));
      byte[] theValue = new byte[len];
      int bytesRead = 0;
      while (bytesRead < len) {
        int n = in.read(theValue, bytesRead, len - bytesRead);
        if (n < 0)
          throw new Exception("Premature end of stream while reading value for key '"+keyOrMarker+"' (read "+bytesRead+" bytes, expecting "+len+")");
        bytesRead += n;
      }

      byte b = (byte)in.read();
      if (b != ' ')
        throw new Exception("Expecting a space separator after value for key '"+keyOrMarker+"'");

      newRow.put(keyOrMarker, theValue);
    }
  }

  public static Row readFrom(RandomAccessFile in) throws Exception {
    String theKey = readStringSpace(in);
    if (theKey == null)
      return null;

    Row newRow = new Row(theKey);
    while (true) {
      String keyOrMarker = readStringSpace(in);
      if (keyOrMarker == null)
        return newRow;

      int len = Integer.parseInt(readStringSpace(in));
      byte[] theValue = new byte[len];
      int bytesRead = 0;
      while (bytesRead < len) {
        int n = in.read(theValue, bytesRead, len - bytesRead);
        if (n < 0)
          throw new Exception("Premature end of stream while reading value for key '"+keyOrMarker+"' (read "+bytesRead+" bytes, expecting "+len+")");
        bytesRead += n;
      }

      byte b = (byte)in.read();
      if (b != ' ')
        throw new Exception("Expecting a space separator after value for key '"+keyOrMarker+"'");

      newRow.put(keyOrMarker, theValue);
    }
  }

  public String toString() {
    String s = key+" {";
    boolean isFirst = true;
    for (String k : values.keySet()) {
      s = s + (isFirst ? " " : ", ")+k+": "+new String(values.get(k));
      isFirst = false;
    }
    return s + " }";
  }

  public synchronized byte[] toByteArray() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      // 写入行键
      baos.write(key.getBytes());
      baos.write(' ');  // 空格分隔符

      // 写入每一列的键和值（获取每列的最新版本）
      for (String column : columns.keySet()) {
        TreeMap<Integer, byte[]> versions = columns.get(column);
        int latestVersion = versions.lastKey();  // 获取最新版本号
        byte[] value = versions.get(latestVersion);  // 获取最新版本的值

        // 打印列名和数据值长度

        // 写入列名
        baos.write(column.getBytes());  // 列名
        baos.write(' ');  // 空格分隔符

        // 写入值的长度
        baos.write(Integer.toString(value.length).getBytes());  // 值的长度
        baos.write(' ');  // 空格分隔符

        // 写入实际的值
        baos.write(value);  // 值
        baos.write(' ');  // 空格分隔符
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("This should not happen!");
    }

    return baos.toByteArray();
  }

  public static Row fromByteArray(byte[] data) throws Exception {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    return readFrom(bais);  // 使用现有的 readFrom(InputStream) 方法
  }
}