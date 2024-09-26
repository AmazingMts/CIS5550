package cis5550.webserver;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.net.*;
import java.nio.charset.*;

// Provided as part of the framework code

class RequestImpl implements Request {
  String method;
  String url;
  String protocol;
  InetSocketAddress remoteAddr;
  Map<String,String> headers;
  Map<String,String> queryParams;
  Map<String,String> params;
  byte bodyRaw[];
  Server server;

  RequestImpl(String methodArg, String urlArg, String protocolArg, Map<String,String> headersArg, Map<String,String> queryParamsArg, Map<String,String> paramsArg, InetSocketAddress remoteAddrArg, byte bodyRawArg[], Server serverArg) {
    method = methodArg;
    url = urlArg;
    remoteAddr = remoteAddrArg;
    protocol = protocolArg;
    headers = new HashMap<>();
    for (Map.Entry<String, String> entry : headersArg.entrySet()) {
      headers.put(entry.getKey().toLowerCase(), entry.getValue());
    }

    queryParams = queryParamsArg;
    params = paramsArg;
    bodyRaw = bodyRawArg;
    server = serverArg;
  }

  public String requestMethod() {
    return method;
  }
  public void setParams(Map<String,String> paramsArg) {
    params = paramsArg;
  }
  public int port() {
    return remoteAddr.getPort();
  }
  public String url() {
    return url;
  }
  public String protocol() {
    return protocol;
  }
  public String contentType() {
    return headers.get("content-type");
  }
  public String ip() {
    return remoteAddr.getAddress().getHostAddress();
  }
  public String body() {
    return new String(bodyRaw, StandardCharsets.UTF_8);
  }
  public byte[] bodyAsBytes() {
    return bodyRaw;
  }
  public int contentLength() {
    return bodyRaw.length;
  }

  public String headers(String name) {
    return headers.get(name.toLowerCase());
  }
  public Set<String> headers() {
    return headers.keySet();
  }
  public String params(String param) {
    return params.get(param);
  }
  public Map<String,String> params() {
    return params;
  }
  private void parseQueryParams() {
    String[] urlParts = url.split("\\?");
    if (urlParts.length > 1) {
      String queryString = urlParts[1];
      String[] params = queryString.split("&");
      for (String param : params) {
        String[] keyValue = param.split("=");
        String key = decode(keyValue[0]); // 解码键
        String value = keyValue.length > 1 ? decode(keyValue[1]) : null; // 解码值，如果没有值则为 null
        queryParams.put(key, value); // 将解码后的键和值放入 queryParams
      }
    }
  }

  // URL 解码
  private String decode(String value) {
    try {
      return URLDecoder.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return value;  // 如果解码失败，返回原值
    }
  }


  private void parseBodyParams() {
    if (bodyRaw != null && bodyRaw.length > 0) {
      // 将 byte[] 转换为字符串
      String bodyString = new String(bodyRaw, StandardCharsets.UTF_8);
      String[] bodyParams = bodyString.split("&");

      for (String param : bodyParams) {
        String[] keyValue = param.split("=");
        if (keyValue.length == 2) {
          String key = keyValue[0];
          String value = keyValue[1];
          queryParams.put(key, value);
        }
      }
    }
  }


  // 获取查询参数
  public String queryParams(String name) {
    parseQueryParams();
    parseBodyParams();
    return queryParams.get(name); // 返回指定参数的值
  }

  // 获取所有查询参数的键
  public Set<String> queryParams() {
    parseBodyParams();
    return queryParams.keySet();
  }
}

