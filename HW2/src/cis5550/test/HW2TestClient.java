package cis5550.test;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;

public class HW2TestClient extends GenericTest {

  HW2TestClient() {
    super();
  }

  void prompt() {

    /* Ask the user to confirm that the server is running */

    System.out.println("In another terminal window, please run 'java cis5550.test.HW2TestServer, and then hit Enter in this window to continue.");
    (new Scanner(System.in)).nextLine();
  }

  void runSetup() throws Exception {
    Path path = Paths.get("__test123");
    if (!Files.exists(path)) 
      (new File("__test123")).mkdir();

    PrintWriter p = new PrintWriter("__test123"+File.separator+"file1.txt");
    p.print("Well done is better than well said.");
    p.close();
  }

  void cleanup() {
    File a = new File("__test123"+File.separator+"file1.txt");
    a.delete();

    File subdir = new File("__test123");
    subdir.delete();
  }

  void runTests(Set<String> tests) throws Exception {

    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("static")) try {
      startTest("static", "Request a static file", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body)+"\nCheck your path-matching code to make sure that the route for /hello is actually being triggered.");
      if (!r.body().equals("Well done is better than well said.")) 
        testFailed("The server returned a response, but it was not the Benjamin Franklin quote we expected. Here is what we received instead:\n\n"+dump(r.body)+"\nCheck whether you are sending two (!) CRLFs at the end of the headers (not just one, and not just a CR), and whether you need to flush the OutputStream.");
      s.close();
      s = openSocket(8080);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /thisFileShouldNotExist.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 404)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 404 Not Found, because the file we requested should not exist. Here is what was in the body:\n\n"+dump(r.body));
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("get")) try {
      startTest("get", "Single GET request", 15);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /hello HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body)+"\nCheck your path-matching code to make sure that the GET route for /hello is actually being triggered.");
      if (!r.body().equals("Hello World")) 
        testFailed("The server returned a response, but it was not the 'Hello World' we expected. Here is what we received instead:\n\n"+dump(r.body)+"\nCheck whether you are sending two (!) CRLFs at the end of the headers (not just one, and not just a CR), and whether you need to flush the OutputStream.");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("post")) try {
      startTest("post", "POST request with a body", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String theBody = randomAlphaNum(5,20);
      out.print("POST /postbody HTTP/1.1\r\nContent-Length: "+theBody.length()+"\r\nHost: localhost:8000\r\n\r\n"+theBody);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body)+"\nCheck your path-matching code to make sure that the POST route for /postbody is actually being triggered.");
      if (!r.body().equals("req.body() returned: "+theBody)) 
        testFailed("The server returned a response, but it was not the string we expected. We sent a request with '"+theBody+"' in the body, and here is what the server sent back:\n\n"+dump(r.body)+"\nCheck whether you are reading the body correctly. Maybe you waited for a CRLF at the end (this is not required), or you didn't look for the Content-Length header?");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("puthdr")) try {
      startTest("puthdr", "PUT request with extra headers", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String theVal = randomAlphaNum(5,20);
      out.print("PUT /puthdr HTTP/1.1\r\nX-Foo: "+theVal+"\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is the entire response:\n\n"+dump(r.response)+"\nCheck your path-matching code to make sure that the PUT route for /puthdr is actually being triggered.");
      if (!r.body().equals("OK")) 
        testFailed("The server returned a response, but it was not the 'OK' string we expected. Here is the body the server sent back:\n\n"+dump(r.body)+"\nCheck whether you are parsing the headers correctly. Remember to convert everything to lower case!!");
      if (r.headers.get("x-bar") == null)
        testFailed("The server does not appear to have returned the X-Bar header the application was trying to generate. Here is the entire response that the server sent back (including headers):\n\n"+dump(r.response)+"\nCheck whether you are writing out the headers when a normal response is sent, not just when write() is called!");
      if (!r.headers.get("x-bar").equals(theVal))
        testFailed("The server did return the X-Bar header, but it didn't contain what we expected. We had sent the X-Foo header to '"+theVal+"', and the application was supposed to echo back that entire value in the X-Bar header. Here is the entire response that the server sent back (including headers):\n\n"+dump(r.response)+"\nCheck whether the server decoded the header correctly; you may want to add extra debug output to HW2TestServer, to see what the application thought the X-Foo header was.");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("pathpar")) try {
      startTest("pathpar", "Request with path parameters", 10);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String val1 = randomAlphaNum(5,5), val2 = randomAlphaNum(5,5);
      out.print("PUT /pathpar/"+val1+"/123/"+val2+" HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is the entire response:\n\n"+dump(r.response)+"\nCheck your path-matching code to make sure that the PUT route for /puthdr is actually being triggered.");
      if (!r.body().equals("Val1: ["+val1+"] Val2: ["+val2+"]"))
        testFailed("The server returned a response, but it was not the string string we expected. Here is the body the server sent back:\n\n"+dump(r.body)+"\nCheck whether you are parsing the headers correctly. Remember to convert everything to lower case!!");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("error")) try {
      startTest("error", "Exception within the route", 5);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /error HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 500)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 500 Internal Server Error. Here is the entire response:\n\n"+dump(r.response)+"\nCheck that you are invoking the route handler within a try...catch block, and that the catch block generates a 500 response instead of sending back whatever body the route tried to set.");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("write")) try {
      startTest("write", "Using write()", 10);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String val1 = randomAlphaNum(5,5), val2 = randomAlphaNum(5,5);
      out.print("GET /write HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response", false);
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is the entire response:\n\n"+dump(r.response)+"\nCheck your path-matching code to make sure that the GET route for /write is actually being triggered.");
      if (!r.body().equals("Hello World!"))
        testFailed("The server returned a response, but it was not the string 'Hello World!' we expected. Here is the body the server sent back:\n\n"+dump(r.body)+"\nCheck whether you are parsing the headers correctly. Remember to convert everything to lower case!!");
      if (r.headers.get("x-bar") == null)
        testFailed("The server does not appear to have returned the X-Bar header the application was trying to generate. Here is the entire response that the server sent back (including headers):\n\n"+dump(r.response)+"\nCheck whether you are writing out the headers when write() is called!");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("qparam")) try {
      startTest("qparam", "Query parameters in URL and body", 10);
      Socket s = openSocket(8080);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String val1 = randomAlphaNum(5,5), val2 = randomAlphaNum(5,5), val3 = randomAlphaNum(5,5), val4 = randomAlphaNum(5,5);
      out.print("GET /qparam?par1="+val1+"+X&par2=%22"+val2+"%22 HTTP/1.1\r\nHost: localhost:8000\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 21\r\n\r\npar3="+val3+"&par4="+val4);
      out.flush();

      Response r = readAndCheckResponse(s, "response");
      String expectedResponse = val1+" X,\""+val2+"\","+val3+","+val4;
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is the entire response:\n\n"+dump(r.response)+"\nCheck your path-matching code to make sure that the GET route for /write is actually being triggered.");
      if (!r.body().equals(expectedResponse))
        testFailed("The server returned a response, but it was not the string '"+expectedResponse+"' we expected. Here is the body the server sent back:\n\n"+dump(r.body)+"\nIf all four values are null, check whether you are parsing the query parameters correctly. If only the last two are null, you may not be looking in the body, or you may not be checking for the correct MIME type. If you see a plus sign or an ampersand in the response, you are probably missing the URL-decode step. If you aren't sure what these results mean, have a look at the test case and the corresponding route on the server.");
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    System.out.println("--------------------------------------------------------\n");
    if (numTestsFailed == 0)
      System.out.println("Looks like your solution passed all of the selected tests. Congratulations!");
    else
      System.out.println(numTestsFailed+" test(s) failed.");

    closeOutputFile();
  }

	public static void main(String args[]) throws Exception {

    /* Make a set of enabled tests. If no command-line arguments were specified, run all tests. */

    Set<String> tests = new TreeSet<String>();
    boolean runSetup = true, runTests = true, promptUser = true, outputToFile = false, exitUponFailure = true, cleanup = true;

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
    }  else if ((args.length > 0) && args[0].equals("cleanup")) {
      runSetup = false;
      runTests = false;
      promptUser = false;
      cleanup = true;
    } else if ((args.length > 0) && args[0].equals("version")) {
      System.out.println("HW2 autograder v1.1a (Jan 18, 2023)");
      System.exit(1);
    }

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto")) {
      tests.add("static");
    	tests.add("get");
    	tests.add("post");
    	tests.add("puthdr");
    	tests.add("pathpar");
    	tests.add("error");
    	tests.add("write");
    	tests.add("qparam");
    } 

    for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup")  && !args[i].equals("cleanup"))
        tests.add(args[i]);

    HW2TestClient t = new HW2TestClient();
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
