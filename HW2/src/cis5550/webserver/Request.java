package cis5550.webserver;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Request {
  // The methods below should return the IP address and the port number of the client (!). Suppose 
  // your server is running on IP 1.2.3.4 and port 8080, and a client connects from IP 5.6.7.8
  // and port 5555. Then ip() should return the string "5.6.7.8" and port() should return 5555.
  String ip();
  int port();

  // The methods below should return the three elements of the request line. For instance, if the
  // client sent "GET /index.html HTTP/1.1", requestMethod() should return "GET", url() should
  // return "/index.html", and protocol() should return "HTTP/1.1". If query parameters are 
  // present, they should be removed; for instance, url() should still return "/index.html" if
  // the request is "GET /index.html?foo=bar HTTP/1.1".
  String requestMethod();
  String url();
  String protocol();

  // The headers() method should return, in lower case (!), the set of header names that the client 
  // sent at least once. For instance, if the client sent "Content-Length: 37", "Cookie: foo=bar", 
  // and "Cookie: abc=def", headers() should return a set that contains "content-Length" and "cookie". 
  // The other methods below are convenience methods; headers(x) should return 
  // headers().get(x.toLowerCase()), and contentType() should return headers.get("content-type").
  Set<String> headers();
  String headers(String name);
  String contentType();

  // The methods below are used to access the message body (if any). body() should return the data
  // as a string, bodyAsBytes() should return it as a byte array, and contentLength() should return
  // the number of bytes in the body. If the request did not include a body, body() should return "",
  // bodyAsBytes should return a byte array of length zero, and contentLength() should return 0.
  String body();
  byte[] bodyAsBytes();
  int contentLength();

  // The methods below are used to access query parameters. Query parameters are sent as a string
  // of URL-encoded key-value pairs, separated with an ampersand (&). For instance, "foo=x%20y&abc=123"
  // contains two keys, 'foo' and 'abc', which have the values "x y" and "123", respectively. Notice
  // that the space in "x y" has been replaced by "%20", since spaces are not allowed in URLs; you
  // can use java.net.URLDecoder.decode() to decode strings that have been encoded in this way.
  // Query parameters can be in two places: 1) the part of the URL after the question mark, if any,
  // and 2) the body of the request, _if_ the Content-Type is set to 'application/x-www-form-urlencoded'.
  // Your implementation should look in both places, and return all key-value pairs it finds. 
  // queryParams() should return a set with all the keys, and queryParams(X) should return the
  // value(s) for key X. If a key is present more than once, return the values separated by commas,
  // in any order.
  Set<String> queryParams();
  String queryParams(String param);

  // The methods below are used to access named parameters in URLs. A named parameter is a part of a 
  // route path that starts with a colon (:) and is separated from the rest of the URL with forward 
  // slashes (/). For instance, if a route has path /hello/:name/:x and a client requests /hello/Bob/123, 
  // params() should return a Map that maps "name" to "Bob" and "x" to "123". params(X) should simply
  // return params().get(X). You may assume that all the parameter names are unique.
  Map<String, String> params();
  String params(String name);
};
