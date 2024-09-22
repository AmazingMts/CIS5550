package cis5550.test;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.cert.*;

public class HW3TestClient extends GenericTest {

  HW3TestClient() {
    super();
  }

  void runSetup() throws Exception {
    Path path = Paths.get("keystore.jks");
    if (!Files.exists(path)) {
      Process p = Runtime.getRuntime().exec("keytool -genkeypair -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass secret -dname \"CN=blah, OU=blubb, O=foo, L=Bar, C=US\"");
      p.waitFor();
    }
  }

  void cleanup() {
  }

  void prompt() {
    /* Ask the user to confirm that the server is running */

    System.out.println("In another terminal window, please run 'java cis5550.test.HW3TestServer, and then hit Enter in this window to continue.");
    (new Scanner(System.in)).nextLine();
  }

  void runTests(Set<String> tests) throws Exception {
    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("https")) try {
      startTest("https", "GET request via HTTPS", 10);

      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      } };

      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      SSLSocketFactory oldFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      Random r = new Random();
      Integer i = r.nextInt(10000);
      URL url = new URL("https://localhost:8443/echo/"+i);
      HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
      con.setRequestMethod("GET");
      con.setConnectTimeout(1000);
      con.setReadTimeout(1000);
      con.setHostnameVerifier(new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });
      con.connect();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      InputStream in = con.getInputStream();
      byte[] buf = new byte[100000];
      while (true) {
        int n = in.read(buf);
        if (n<0)
          break;
        baos.write(buf, 0, n);
      }

      HttpsURLConnection.setDefaultSSLSocketFactory(oldFactory);

      String response = new String(baos.toByteArray());
      if (!response.equals(""+i))
        testFailed("The server returned \""+response+"\", but we expected \""+i+"\"");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("http")) try {
      startTest("http", "GET request via HTTP", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      Random rx = new Random();
      Integer i = rx.nextInt(10000);
      out.print("GET /echo/"+i+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals(""+i)) 
        testFailed("The server returned a response, but it was not the '"+i+"' we expected. Here is what we received instead:\n\n"+dump(r.body));
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("sessionid")) try {
      startTest("sessionid", "Session ID generation", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /session HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") == null)
        testFailed("The server does not appear to have returned a Set-Cookie header at all. Check the implementation of your session() method.");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("permanent")) try {
      startTest("permanent", "Session permanence", 10);
      Socket s = openSocket(8080);
      String val = randomAlphaNum(5,10);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /perm/"+val+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for the first request, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") == null)
        testFailed("The server does not appear to have returned a Set-Cookie header for the first request, without the session cookie. Check the implementation of your session() method.");
      if (!r.body().equals(val))
        testFailed("The server returned something other than the expected value, '"+val+"', which was:\n\n"+dump(r.body)+"\nThis was supposed to be a fresh session, so it should not have contained any key-value pairs. Check your 'attribute()' implementation.");
      String setCookie = r.headers.get("set-cookie");
      if (!setCookie.contains("SessionID="))
        testFailed("The Set-Cookie: header was supposed to set a cookie called 'SessionID', but it looks like it didn't - the header we got was '"+setCookie+"'. Check your session ID code.");
      String cookie = setCookie.substring(setCookie.indexOf("SessionID=")+10).split(";")[0];
      s.close();

      s = openSocket(8080);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /perm/"+val+" HTTP/1.1\r\nHost: localhost\r\nCookie: SessionID="+cookie+"\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for the second request, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") != null)
        testFailed("The server appears to have returned a Set-Cookie header for the second request, even though we did send a Cookie header with the session ID. Check the code that looks up existing sessions.");
      if (!r.body().equals(val))
        testFailed("The server returned something other than the expected value, '"+val+"', which was:\n\n"+dump(r.body)+"\nThis was supposed to be an existing session, so the value from the earlier request should have been preserved. Maybe you are creating a fresh session for some reason?");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("expire")) try {
      startTest("expire", "Session expiration", 5);
      Socket s = openSocket(8080);
      String val = randomAlphaNum(5,10);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /perm/"+val+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for the first request, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") == null)
        testFailed("The server does not appear to have returned a Set-Cookie header for the first request, without the session cookie. Check the implementation of your session() method.");
      if (!r.body().equals(val))
        testFailed("The server returned something other than the expected value, '"+val+"', which was:\n\n"+dump(r.body)+"\nThis was supposed to be a fresh session, so it should not have contained any key-value pairs. Check your 'attribute()' implementation.");
      String setCookie = r.headers.get("set-cookie");
      if (!setCookie.contains("SessionID="))
        testFailed("The Set-Cookie: header was supposed to set a cookie called 'SessionID', but it looks like it didn't - the header we got was '"+setCookie+"'. Check your session ID code.");
      String cookie = setCookie.substring(setCookie.indexOf("SessionID=")+10).split(";")[0];
      s.close();

      s = openSocket(8080);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /perm/"+val+" HTTP/1.1\r\nHost: localhost\r\nCookie: SessionID="+cookie+"\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for the second request, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") != null)
        testFailed("The server appears to have returned a Set-Cookie header for the second request, even though we did send a Cookie header with the session ID. Check the code that looks up existing sessions.");
      if (!r.body().equals(val))
        testFailed("The server returned something other than the expected value, '"+val+"', which was:\n\n"+dump(r.body)+"\nThis was supposed to be an existing session, so the value from the earlier request should have been preserved. Maybe you are creating a fresh session for some reason?");
      s.close();

      Thread.sleep(1500);

      s = openSocket(8080);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /perm/"+val+" HTTP/1.1\r\nHost: localhost\r\nCookie: SessionID="+cookie+"\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for the second request, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("set-cookie") == null)
        testFailed("The server does not appear to have returned a Set-Cookie header for the third request. Check whether sessions are being expired correctly.");
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    System.out.println("--------------------------------------------------------\n");
    if (numTestsFailed == 0)
      System.out.println("Looks like your solution passed all of the selected tests. Congratulations!");
    else
      System.out.println(numTestsFailed+" test(s) failed.");

    cleanup();
    closeOutputFile();
  }

	public static void main(String args[]) throws Exception {

    /* Make a set of enabled tests. If no command-line arguments were specified, run all tests. */

    Set<String> tests = new TreeSet<String>();
    boolean runSetup = true, runTests = true, promptUser = true, outputToFile = false, exitUponFailure = true, cleanup = false;

    if ((args.length > 0) && args[0].equals("auto")) {
      runSetup = false;
      outputToFile = true;
      exitUponFailure = false;
      promptUser = false;
      cleanup = false;
    } else if ((args.length > 0) && args[0].equals("setup")) {
      runSetup = true;
      runTests = false;
      promptUser = false;
      cleanup = false;
    } else if ((args.length > 0) && args[0].equals("cleanup")) {
      runSetup = false;
      runTests = false;
      promptUser = false;
      cleanup = true;
    } else if ((args.length > 0) && args[0].equals("version")) {
      System.out.println("HW3 autograder v1.1a (Jan 18, 2023)");
      System.exit(1);
    }

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto")) {
      tests.add("https");
      tests.add("http");
      tests.add("sessionid");
      tests.add("permanent");
      tests.add("expire");
    } 

    for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup")  && !args[i].equals("cleanup"))
        tests.add(args[i]);

    HW3TestClient t = new HW3TestClient();
    t.setExitUponFailure(exitUponFailure);
    if (outputToFile)
      t.outputToFile();
    if (runSetup)
      t.runSetup();
    if (promptUser)
      t.prompt();
    if (runTests)
      t.runTests(tests);
    if (cleanup)
      t.cleanup();
  }
}
