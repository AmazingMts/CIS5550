package cis5550.webserver;

import java.util.AbstractMap;
import java.util.List;

public interface Response {

  // The methods below are used to set the body, either as a string or as an array of bytes
  // (if the application wants to return something binary - say, an image file). Your server
  // should send back the following in the body of the response:
  //   * If write() has been called, ignore both the return value of Route.handle() and
  //     any calls to body() and bodyAsBytes().
  //   * If write() has not been called and Route.handle() returns something other than null,
  //     call the toString() method on that object and send the result.
  //   * If write() has not been called and Route.handle returns null(), use the value from
  //     the most recent body() or bodyAsBytes() call.
  //   * If none of write(), body(), and bodyAsBytes() have been called and Route.handle returns null,
  //     do not send a body in the response.
  void body(String body);
  void bodyAsBytes(byte bodyArg[]);

  // This method adds a header. For instance, header("Cookie", "abc=def") should cause your
  // server to eventually send a header line "Cookie: abc=def". This method can be called
  // multiple times with the same header name; the result should be multiple header lines.
  // If write() has been called, this method should have no effect.
  void header(String name, String value);

  // This method sets the Content-Type: header. If type() isn't called, the default should
  // be 'text/html'.
  void type(String contentType);

  // This method sets the status code and the reason phrase. If it is called more than once,
  // use the latest values. If it is never called, use 200 and "OK". If write() has been
  // called, status() should have no effect.
  void status(int statusCode, String reasonPhrase);

  // This method can be used to send data directly to the connection, without buffering it
  // in an object in memory. The first time write() is called, it should 'commit' the
  // response by sending out the status code/reason phrase and any headers that have been
  // set so far. Your server should 1) add a 'Connection: close' header, and it should
  // 2) NOT add a Content-Length header in this case. Then, and in any subsequent calls,
  // it should simply write the provided bytes directly to the connection.
  void write(byte[] b) throws Exception;

  // EXTRA CREDIT ONLY - please see the handout for details. If you are not doing the extra
  // credit, please implement this with a dummy method that does nothing.
  void redirect(String url, int responseCode);

  // EXTRA CREDIT ONLY - please see the handout for details. If you are not doing the extra
  // credit, please implement this with a dummy method that does nothing.
  void halt(int statusCode, String reasonPhrase);
  int getStatusCode();
  String getStatusMessage();
  List<AbstractMap.SimpleEntry<String, String>> getHeaders();
  String getContentType();
  byte[] getBody();
};
