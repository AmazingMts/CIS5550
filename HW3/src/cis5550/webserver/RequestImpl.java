package cis5550.webserver;

import java.util.*;
import java.net.*;
import java.nio.charset.*;
import cis5550.webserver.YourRunnable;
// Provided as part of the framework code

class RequestImpl implements Request {
  String method;
  String url;
  String protocol;
  InetSocketAddress remoteAddr;
  Map<String,String> headers;
  Map<String,String> queryParams;
  Map<String,String> params;
  byte bodyRaw[]="yourBodyHere".getBytes();
  Server server;
  private Session currentSession = null;
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

  @Override

  public String cookie(){return headers.get("Cookie");}
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
    // Return value from URL params or body params
    String value = queryParams.get(param);
    if (value == null && "application/x-www-form-urlencoded".equals(contentType())) {
      String bodyStr = body();
      String[] pairs = bodyStr.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2 && keyValue[0].equals(param)) {
          value = keyValue[1];
          break;
        }
      }
    }
    return value;
  }

  public Set<String> queryParams() {
    // Combine query parameters from URL and body
    Map<String, String> combinedParams = new HashMap<>(queryParams);

    // Parse body parameters if content type is "application/x-www-form-urlencoded"
    if ("application/x-www-form-urlencoded".equals(contentType())) {
      String bodyStr = body();
      String[] pairs = bodyStr.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          combinedParams.put(keyValue[0], keyValue[1]);
        }
      }
    }
    return combinedParams.keySet();
  }

  public String params(String param) {
    return params.get(param);
  }

  @Override
  public Session session() {
    if (currentSession == null) {
      // 创建一个虚拟的 Response 对象，仅用于传递到 server.session()，不用于实际响应
      Response dummyResponse = new ResponseImpl();
      boolean isHttps=url.startsWith("https");
      currentSession = YourRunnable.session(this, dummyResponse,isHttps);
    }
    return currentSession;
  }

  public Map<String,String> params() {
    return params;
  }

}

