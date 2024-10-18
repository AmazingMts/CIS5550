package cis5550.tools;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.X509Certificate;

// This class is NOT thread-safe! If you have more than one thread, each thread should get its own instance.

public class HTTP {
  public static class Response {
    byte body[];
    Map<String,String> headers;
    int statusCode;

    public Response(byte bodyArg[], Map<String,String> headersArg, int statusCodeArg) {
      body = bodyArg;
      headers = headersArg;
      statusCode = statusCodeArg;
    }

    public byte[] body() {
      return body;
    }

    public int statusCode() {
      return statusCode;
    }

    public Map<String,String> headers() {
      return headers;
    }
  }

  static Map<String,Vector<Socket>> cachedConnections;

  static Socket openSocket(String protocol, String host, int port) {
    try {
      if (protocol.equals("https")) {
        TrustManager[] trustAllCerts = { new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() { return null; }
          public void checkClientTrusted(X509Certificate[] certs, String authType) { }
          public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        } };
        SSLContext sc;
        try { 
          sc = SSLContext.getInstance("SSL"); 
          sc.init(null, trustAllCerts, new SecureRandom());
          return sc.getSocketFactory().createSocket(host, port);
        } catch (NoSuchAlgorithmException nsae) {
        } catch (KeyManagementException kme) {
        }
      } else if (protocol.equals("http")) {
        return new Socket(host, port);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public static Response doRequest(String method, String urlArg, byte uploadOrNull[]) throws IOException {
    return doRequestWithTimeout(method, urlArg, uploadOrNull, -1, false);
  }

  public static Response doRequestWithTimeout(String method, String urlArg, byte uploadOrNull[], int timeoutMillis, boolean isHeadRequest) throws IOException {
    String protocol = "http";
    int pos = urlArg.indexOf("://");
    if (pos >= 0) {
      protocol = urlArg.substring(0, pos);
      urlArg = urlArg.substring(pos+3);
    }
    pos = urlArg.indexOf('/');
    if (pos < 0)
      return null;

    String host = urlArg.substring(0, pos), path = urlArg.substring(pos);
    int port = (protocol.equals("https")) ? 443 : 80;
    pos = host.indexOf(":");
    if (pos > 0) {
      String sport = host.substring(pos+1);
      try { port = Integer.valueOf(sport).intValue(); } catch (NumberFormatException nfe) {}
      host = host.substring(0, pos);
    }

    String serverID = protocol+"-"+host+"-"+port;

    while (true) {
      boolean usingCached = false;
      Socket sock = null;
      if ((cachedConnections != null) && (cachedConnections.get(serverID) != null)) {
        synchronized(cachedConnections) {
          if (cachedConnections.get(serverID).size() > 0)
            sock = cachedConnections.get(serverID).remove(0);
        }
        if (sock != null)
          usingCached = true;
      }
      if (sock == null)
        sock = openSocket(protocol, host, port);
      if (sock == null)
        throw new IOException("Cannot connect to server "+host+":"+port);

      try {
        if (timeoutMillis > 0)
          sock.setSoTimeout(timeoutMillis);

        OutputStream out = sock.getOutputStream();
        String request = method+" "+path+" HTTP/1.1\r\nHost: "+host+"\r\n";
//  System.out.println((usingCached ? "[CACHED]" : "[FRESH]")+request);
      if (uploadOrNull != null)
          request = request + "Content-Length: "+uploadOrNull.length+"\r\n";
        request = request + "Connection: keep-alive\r\n\r\n";
        out.write(request.getBytes());
        if (uploadOrNull != null)
          out.write(uploadOrNull);
        out.flush();
      } catch (IOException ioe) {
        try { sock.close(); } catch (Exception e) {}
        if (usingCached) {
          System.out.println("XXX cached connection failed; continuing with normal connection");
          continue;
        }
        throw new IOException("Connection to "+host+":"+port+" failed while writing the request");
      }

  
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      boolean readingHeaders = true;
      int contentLength = -1;
      Map<String,String> headers = new HashMap<String,String>();
      byte buf[] = new byte[100000];
      int inBuf = 0;
      int statusCode = -1;

      try {
        InputStream in = sock.getInputStream();
        while (true) {
          int n = in.read(buf, inBuf, buf.length - inBuf);
          if (n<0)
            break;

          inBuf += n;
          if (readingHeaders) {
            int matchPtr = 0;
            for (int i=0; i<inBuf; i++) {
              if (buf[i] == 10)
                matchPtr ++;
               else if (buf[i] != 13)
                matchPtr = 0;

              if (matchPtr == 2) {
                buffer.write(buf, 0, i);
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer.toByteArray());
                BufferedReader hdr = new BufferedReader(new InputStreamReader(bais));
                String statusLine[] = hdr.readLine().split(" ");
                statusCode = Integer.valueOf(statusLine[1]);

                while (true) {
                  String s = hdr.readLine();
                  if (s.equals(""))
                    break;
                  String[] p2 = s.split(":", 2);
                  if (p2.length == 2) {
                    String headerName = p2[0];
                    headers.put(headerName.toLowerCase(), p2[1].trim());
                    if (headerName.toLowerCase().equals("content-length"))
                      contentLength = Integer.parseInt(p2[1].trim());
                  }
                }
                buffer.reset();
                System.arraycopy(buf, i+1, buf, 0, inBuf-(i+1));
                inBuf -= (i+1);
                readingHeaders = false;
                break;
              }
            }
          }

          if (!readingHeaders) {
            int toCopy = ((contentLength>=0) && (inBuf > contentLength)) ? contentLength : inBuf;
            buffer.write(buf, 0, toCopy);
            System.arraycopy(buf, toCopy, buf, 0, inBuf-toCopy);
            inBuf -= toCopy;

            if ((buffer.size() >= contentLength) || isHeadRequest)
              break;
          }
        }
      } catch (Exception e) {
        try { sock.close(); } catch (Exception e2) {}
        e.printStackTrace();
        throw new IOException("Connection to "+host+":"+port+" failed while reading the response ("+e+")");
      }

      if (cachedConnections == null) 
        cachedConnections = new HashMap<String,Vector<Socket>>();
      synchronized(cachedConnections) {
        if (cachedConnections.get(serverID) == null)
          cachedConnections.put(serverID, new Vector<Socket>());
        cachedConnections.get(serverID).add(sock);
      }

      return new Response(buffer.toByteArray(), headers, statusCode);
    }
  }
}