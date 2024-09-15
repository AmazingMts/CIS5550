package cis5550.webserver;

import java.util.AbstractMap;
import java.util.List;

public interface Response {

  void body(String body);
  void bodyAsBytes(byte bodyArg[]);

  void header(String name, String value);

  void type(String contentType);

  void status(int statusCode, String reasonPhrase);

  void write(byte[] b) throws Exception;

  void redirect(String url, int responseCode);

  void halt(int statusCode, String reasonPhrase);
  int getStatusCode();
  String getStatusMessage();
  List<AbstractMap.SimpleEntry<String, String>> getHeaders();
  String getContentType();
  byte[] getBody();
};
