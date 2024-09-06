package cis5550.test;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;

public class HW1Test extends GenericTest {

  HW1Test() {
    super();
  }

  void cleanup() {
    File a = new File("test"+File.separator+"file1.txt");
    File b = new File("test"+File.separator+"file2.txt");
    File c = new File("test"+File.separator+"file3.txt");
    File d = new File("test"+File.separator+"binary");
    a.delete();
    b.delete();
    c.delete();
    d.delete();

    File subdir = new File("test");
    subdir.delete();
  }

  void runSetup() throws Exception {

    /* Check whether the test directory already exists. We don't want to accidentally overwrite anything. */

    Path path = Paths.get("test");
    if (!Files.exists(path)) {
      File subdir = new File("test");
      subdir.mkdir();
    }

    PrintWriter p = new PrintWriter("test"+File.separator+"file1.txt");
    p.print("Well done is better than well said.\n");
    p.close();

    p = new PrintWriter("test"+File.separator+"file2.txt");
    p.print("No gains without pains.\n");
    p.close();

    p = new PrintWriter("test"+File.separator+"file3.txt");
    p.print("Lost time is never found again.\n");
    p.close();

    FileOutputStream fos = new FileOutputStream("test"+File.separator+"binary");
    for (int i=0; i<256; i++)
      fos.write(i);
    fos.close();

  }

  void prompt() {
    File subdir = new File("test");

    /* Ask the user to confirm that the server is running */

    System.out.println("In another terminal window, please run 'java cis5550.webserver.Server 8000 "+subdir.getAbsolutePath()+"', and then hit Enter in this window to continue.");
    (new Scanner(System.in)).nextLine();
  }

  void runTests(Set<String> tests) throws Exception {
    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("req")) try {
      startTest("req", "Single request", 15);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HT");
      out.flush();
      Thread.sleep(200);
      assertNoResponseYet(s, "So far we haven't sent a full request, just 'GET /file1.txt HT', so the server should keep reading until it sees the double CRLF that indicates the end of the headers. However, it did send something back already; see below. Please double-check that you keep reading more data in a loop until you see the double CRLF!");
      out.print("TP/1.1\r\n");
      out.flush();
      Thread.sleep(200);
      assertNoResponseYet(s, "So far we haven't sent a full request, just 'GET /file1.txt HTTP/1.1<CRLF>', so the server should keep reading until it sees the double (!) CRLF that indicates the end of the headers. However, it did send something back already; see below. Please double-check that you keep reading more data in a loop until you see the double CRLF!");
      out.print("Host: localhost:8000\r\n\r\n");
      out.flush();
      readAndCheckResponse(s, "response");
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("persist")) try {
      startTest("persist", "Persistent connection", 10);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r1 = readAndCheckResponse(s, "first response (file1.txt)");
      if (r1.statusCode != 200)
        testFailed("We sent a GET /file1.txt. The server was supposed to return status code 200, but it actually returned "+r1.statusCode);
      if (!(new String(r1.body)).equals("Well done is better than well said.\n"))
        testFailed("The server was supposed to send file1.txt, but we got something different. Here is what we received:\n\n"+dump(r1.body));
      out.print("GET /file2.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r2 = readAndCheckResponse(s, "second response (file2.txt)");
      if (r2.statusCode != 200)
        testFailed("The server was supposed to return status code 200, but it actually returned "+r2.statusCode);
      if (!(new String(r2.body)).equals("No gains without pains.\n"))
        testFailed("The server was supposed to send file2.txt, but we got something different. Here is what we received:\n\n"+dump(r2.body));
      out.print("GET /file3.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r3 = readAndCheckResponse(s, "third response (file3.txt)");
      if (r3.statusCode != 200)
        testFailed("The server was supposed to return status code 200, but it actually returned "+r3.statusCode);
      if (!(new String(r3.body)).equals("Lost time is never found again.\n"))
        testFailed("The server was supposed to send file3.txt, but we got something different. Here is what we received:\n\n"+dump(r3.body));
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("err400")) try {
      startTest("err400", "Error 400 (Bad Request)", 2);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("NONSENSE\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 400)
        testFailed("The server was supposed to return status code 400, but it actually returned "+r.statusCode);
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("err404")) try {
      startTest("err404", "Error 404 (Not Found)", 2);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /some-file-that-does-not-exist.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 404)
        testFailed("The server was supposed to return status code 404, but it actually returned "+r.statusCode);
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("err405")) try {
      startTest("err405", "Error 405 (Method Not Allowed)", 2);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("POST /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 405)
        testFailed("The server was supposed to return status code 405, but it actually returned "+r.statusCode);
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("err501")) try {
      startTest("err501", "Error 501 (Not Implemented)", 2);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("EAT /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 501)
        testFailed("The server was supposed to return status code 501, but it actually returned "+r.statusCode);
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("err505")) try {
      startTest("err505", "Error 505 (Version Not Supported)", 2);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HTTP/5.2\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 505)
        testFailed("The server was supposed to return status code 505, but it actually returned "+r.statusCode);
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("withbody")) try {
      startTest("withbody", "Multiple GET requests with bodies", 5);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HTTP/1.1\r\nContent-Length: 5\r\nHost: localhost:8000\r\n\r\nHello");
      out.flush();
/*Response r1 = readAndCheckResponse(s, "first response (file1.txt)");
if (r1.statusCode != 200)
    testFailed("We sent a GET /file1.txt. The server was supposed to return status code 200, but it actually returned "+r1.statusCode);
if (!(new String(r1.body)).equals("Well done is better than well said.\n"))
    testFailed("The server was supposed to send file1.txt, but we got something different. Here is what we received:\n\n"+dump(r1.body)); */
      readAndCheckResponse(s, "first response (file1.txt)");
      out.print("GET /file2.txt HTTP/1.1\r\nContent-Length: 14\r\nHost: localhost:8000\r\n\r\nThis is a test");
      out.flush();
      readAndCheckResponse(s, "second response (file2.txt)");
      out.print("GET /file3.txt HTTP/1.1\r\nContent-Length: 0\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      readAndCheckResponse(s, "third response (file3.txt)");
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("text")) try {
      startTest("text", "Request for a text file", 5);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      if (r.statusCode != 200)
        testFailed("The server was supposed to return a 200 OK, but it actually returned at "+r.statusCode);
      if (r.body.length != 36)
        testFailed("The server was supposed to send 36 bytes, but it actually sent "+r.body.length+". Here is what we received:\n\n"+dump(r.body));
      if (!(new String(r.body)).equals("Well done is better than well said.\n"))
        testFailed("The server was supposed to send file1.txt, but we got something different. Here is what we received:\n\n"+dump(r.body));
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("binary")) try { 
      startTest("binary", "Request for a binary file", 5);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /binary HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      s.shutdownOutput();
      assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      if (r.statusCode != 200)
        testFailed("The server was supposed to return a 200 OK, but it actually returned at "+r.statusCode);
      if (r.body.length != 256)
        testFailed("The server was supposed to send 256 bytes, but it actually sent "+r.body.length+". Here is what we received:\n\n"+dump(r.body));
      for (int i=0; (i<256) && (i<r.body.length); i++) {
        int theByte = (256+r.body[i])%256;
        if (i != theByte)
          testFailed("The server was supposed to send the file 'binary', but we got something different at position "+i+". Here is what we received:\n\n"+dump(r.body));
      }
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    if (tests.contains("multi")) try {
      startTest("multi", "Multiple requests in parallel", 7);
      Socket s1 = openSocket(8000);
      PrintWriter out1 = new PrintWriter(s1.getOutputStream());
      out1.print("GET /file1.txt HTTP/1.1\r\n");
      out1.flush();
      Socket s2 = openSocket(8000);
      PrintWriter out2 = new PrintWriter(s2.getOutputStream());
      out2.print("GET /file2.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
      out2.flush();
      Response r2 = readAndCheckResponse(s2, "second response");
      if (r2.statusCode != 200)
        testFailed("The server was supposed to return a 200 OK in the second connection, but it actually returned at "+r2.statusCode);
      if (r2.body.length != 24)
        testFailed("The second request was supposed to return file2.txt, but we got something different. Here is what we received:\n\n"+dump(r2.body));
      s2.shutdownOutput();
      assertClosed(s2, "The server was supposed to close the second connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      out1.print("Host: localhost:8000\r\n\r\n");
      out1.flush();
      Response r1 = readAndCheckResponse(s1, "first response");
      s1.shutdownOutput();
      assertClosed(s1, "The server was supposed to close the first connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
      if (r1.statusCode != 200)
        testFailed("The server was supposed to return a 200 OK in the first connection, but it actually returned at "+r1.statusCode);
      if (r1.body.length != 36)
        testFailed("The first request was supposed to return file1.txt, but we got something different. Here is what we received:\n\n"+dump(r2.body));
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }


    if (tests.contains("stress")) try {
      startTest("stress", "Send 1,000 requests", 5);
      for (int i=0; i<1000; i++) {
        Socket s = openSocket(8000);
        PrintWriter out = new PrintWriter(s.getOutputStream());
        out.print("GET /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
        out.flush();
        Response r = readAndCheckResponse(s, "response");
        s.shutdownOutput();
        assertClosed(s, "The server was supposed to close the connection when the client closed its end, but it looks like it has not.", "The server seems to be sending more data after the end of the response! You may want to check your Content-Length header.");
        if (r.statusCode != 200)
          testFailed("The server was supposed to return a 200 OK, but it actually returned at "+r.statusCode);
        if (r.body.length != 36)
          testFailed("The server was supposed to send 36 bytes, but it actually sent "+r.body.length+". Here is what we received:\n\n"+dump(r.body));
        if (!(new String(r.body)).equals("Well done is better than well said.\n"))
          testFailed("The server was supposed to send file1.txt, but we got something different. Here is what we received:\n\n"+dump(r.body));
        s.close();
      }
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    boolean stress2ran = false;
    long reqPerSec = 0;
    long numFail = 0;
    if (tests.contains("stress2")) try {
      startTest("stress2", "Send 10,000 persistent requests", 3);
      final int numThreads = 30;
      final int testsTotal = 10000;
      final Counter testsRemaining = new Counter(testsTotal);
      final Counter testsFailed = new Counter(0);
      final Counter testsSucceeded = new Counter(0);
      Thread t[] = new Thread[numThreads];
      long tbegin = System.currentTimeMillis();
      for (int i=0; i<numThreads; i++) {
        t[i] = new Thread("Client thread "+i) { public void run() {
          try {
            while (testsRemaining.aboveZero()) {
              Socket s = openSocket(8000);
              PrintWriter out = new PrintWriter(s.getOutputStream());
              while (testsRemaining.decrement()) {
                out.print("GET /file1.txt HTTP/1.1\r\nHost: localhost:8000\r\n\r\n");
                out.flush();
                Response r = readAndCheckResponse(s, "response");
                if ((r.statusCode != 200) || (r.body.length != 36) || !(new String(r.body)).equals("Well done is better than well said.\n"))
                  testsFailed.increment();
                else
                  testsSucceeded.increment();
              }
              s.close();
            } 
          } catch (Exception e) {
            e.printStackTrace(); 
            testsFailed.increment(); 
          }
        } };
        t[i].start();
      }
      for (int i=0; i<numThreads; i++)
        try { t[i].join(); } catch (InterruptedException ie) {}
      long tend = System.currentTimeMillis();

      stress2ran = true;
      numFail = testsFailed.getValue();
      reqPerSec = 1000*testsSucceeded.getValue()/(tend-tbegin);

      if (numFail == 0)
        testSucceeded();
      else
        testFailed(numFail+" failures during 'stress2'");
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); }

    System.out.println("--------------------------------------------------------\n");
    if (numTestsFailed == 0)
      System.out.println("Looks like your solution passed all of the selected tests. Congratulations!");
    else
      System.out.println(numTestsFailed+" test(s) failed.");
    if (stress2ran) 
      System.out.println("\nYour throughput in 'stress2' was "+reqPerSec+" requests/sec, with "+numFail+" failues");

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
      System.out.println("HW1 autograder v1.1b (Jan 21, 2023)");
      System.exit(1);
    }

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto")) {
    	tests.add("req");
    	tests.add("persist");
    	tests.add("err400");
    	tests.add("err404");
      tests.add("err405");
    	tests.add("err501");
    	tests.add("err505");
    	tests.add("withbody");
    	tests.add("text");
    	tests.add("binary");
    	tests.add("multi");
      tests.add("stress");
      tests.add("stress2");
    } 

  	for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup")  && !args[i].equals("cleanup"))
    		tests.add(args[i]);

    HW1Test t = new HW1Test();
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
