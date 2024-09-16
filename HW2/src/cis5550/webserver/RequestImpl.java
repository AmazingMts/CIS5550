package cis5550.webserver;

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
    headers = headersArg;
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
  public String queryParams(String param) {
    String value = queryParams.get(param);
    if (value == null && "application/x-www-form-urlencoded".equals(contentType())) {
      String bodyStr = body();
      String[] pairs = bodyStr.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
          String val = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
          if (key.equals(param)) {
            value = val;
            break;
          }
        }
      }
    }

    return value;
  }

  public Set<String> queryParams() {
    Map<String, String> combinedParams = new HashMap<>(queryParams);

    // Parse body parameters if content type is "application/x-www-form-urlencoded"
    if ("application/x-www-form-urlencoded".equals(contentType())) {
      String bodyStr = body();
      String[] pairs = bodyStr.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
          String val = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
          combinedParams.put(key, val);
        }
      }
    }
    return combinedParams.keySet();
  }

  public String params(String param) {
    return params.get(param);
  }
  public Map<String,String> params() {
    return params;
  }

}

