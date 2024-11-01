package cis5550.test;

import java.util.*;
import java.io.*;
import java.net.*;

public class HW4CoordinatorTest extends GenericTest {

  HW4CoordinatorTest() {
    super();
  }

  void prompt() {
    /* Ask the user to confirm that the server is running */

    System.out.println("In another terminal window, please run 'java cis5550.kvs.Coordinator 8000, and then hit Enter in this window to continue. NOTE: Some of the tests will need to wait for the worker entries on the coordinator to expire, which will take some time. Some of the tests can take up to a minute. Please be patient!!");
    (new Scanner(System.in)).nextLine();
  }

  void runTests(Set<String> tests) throws Exception {
    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("ping")) try {
      startTest("ping", "Register a new worker using /ping", 5);
      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /ping?id=abcde&port=1234 HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The server did return a 200 status code, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();
      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("workers")) try {
      setTimeoutMillis(25000);
      startTest("workers", "List of workers (takes 20 seconds)", 10);
      Thread.sleep(20000);

      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("0\n"))
        testFailed("The server did return a 200 status code, but it was supposed to return '0<LF>', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      Random rand = new Random();
      int num = 4+rand.nextInt(3);

      List<String> workers = new LinkedList<String>();
      for (int i=0; i<num; i++) {
        String workerID = randomAlphaNum(5, 7);
        int port = 1024 + rand.nextInt(15000);

        s = openSocket(8000);
        out = new PrintWriter(s.getOutputStream());
        out.print("GET /ping?id="+workerID+"&port="+port+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response while we were registering the "+(i+1)+".th worker ("+workerID+":"+port+"), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();

        workers.add(workerID+",127.0.0.1:"+port);
      }

      Collections.sort(workers);
      String expected = "";
      for (String str : workers)
        expected = expected + (expected.equals("") ? "" : ",") + str;

      s = openSocket(8000);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for our GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      String[] pieces = r.body().split("\n");
      if (!pieces[0].equals(""+num))
        testFailed("We registered "+num+" workers, so the coordinator's response to our GET /workers was supposed to start with a "+num+"<LF>, but the coordinator returned something else. Here is what was in the body:\n\n"+dump(r.body));
      if (pieces.length != (1+num))
        testFailed("We registered "+num+" workers, so the coordinator's response to our GET /workers was supposed to consist of "+(num+1)+" lines, separated by LFs, but the coordinator returned "+(pieces.length)+" lines. Here is what was in the body:\n\n"+dump(r.body));

      List<String> results = new LinkedList<String>();
      for (int i=1; i<pieces.length; i++)
        results.add(pieces[i]);

      Collections.sort(results);
      String actual = "";
      for (String str : results)
        actual = actual + (actual.equals("") ? "" : ",") + str;

      if (!actual.equals(expected))
        testFailed("We registered "+num+" workers ("+expected+"), so the coordinator's response to our GET /workers was supposed to return these, but we actually got something else. Here is what was in the body:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    setTimeoutMillis(-1);

    if (tests.contains("table")) try {
      startTest("table", "Worker table as HTML", 5);

      Socket s;
      PrintWriter out;
      Response r;
      Random rand = new Random();
      int num = 4+rand.nextInt(3);

      List<String> workers = new LinkedList<String>();
      for (int i=0; i<num; i++) {
        String workerID = randomAlphaNum(5, 7);
        int port = 1024 + rand.nextInt(15000);

        s = openSocket(8000);
        out = new PrintWriter(s.getOutputStream());
        out.print("GET /ping?id="+workerID+"&port="+port+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response while we were registering the "+(i+1)+".th worker ("+workerID+":"+port+"), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();

        workers.add(workerID);
      }

      s = openSocket(8000);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response for our GET /, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("content-type") == null)
        testFailed("In response to our GET /, the server did NOT return a Content-Type: header");
      if (!r.headers.get("content-type").equals("text/html"))
        testFailed("In response to our GET /, the server did return a Content-Type: header, but its value was '"+r.headers.get("content-type")+"', when we expected text/html.");
      if (!r.body().toLowerCase().contains("<html>"))
        testFailed("In response to our GET /, the server did return text/html content, but we couldn't find a <html> tag. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().toLowerCase().contains("<table"))
        testFailed("In response to our GET /, the server did return a HTML page, but we couldn't find a <table> tag. Here is what was in the body:\n\n"+dump(r.body));
      for (String w : workers)
        if (!r.body().contains(w))
          testFailed("In response to our GET /, the server did return a HTML table, but we couldn't find an entry for '"+w+"', which is the ID of a worker we registered. Here is what was in the body:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("expire")) try {
      setTimeoutMillis(45000);
      startTest("expire", "Worker expiration (takes 40 seconds)", 10);

      /* Wait for the list to be empty, and verify that it is */

      Thread.sleep(20000);

      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our initial GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("0\n"))
        testFailed("The server did return a 200 status code to our initial GET /workers, but it was supposed to return '0<LF>', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      /* Register a bunch of workers */

      Random rand = new Random();
      int num = 4+rand.nextInt(3);

      List<String> workers = new LinkedList<String>();
      for (int i=0; i<num; i++) {
        String workerID = randomAlphaNum(5, 7);
        int port = 1024 + rand.nextInt(15000);

        s = openSocket(8000);
        out = new PrintWriter(s.getOutputStream());
        out.print("GET /ping?id="+workerID+"&port="+port+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response while we were registering the "+(i+1)+".th worker ("+workerID+":"+port+"), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code to our worker registration, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();

        workers.add(workerID+",127.0.0.1:"+port);
      }

      /* Verify that the list isn't empty now */

      s = openSocket(8000);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to the second GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.body().startsWith("0"))
        testFailed("The server did return a 200 status code to the second GET /workers, but it was supposed to return something other than 0, now that we've registered a bunch of workers and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      /* Wait for the entries to expire again, and verify that they have */

      Thread.sleep(20000);

      s = openSocket(8000);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our third GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("0\n"))
        testFailed("The server did return a 200 status code to our third GET /workers, but it was supposed to return '0<LF>', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    setTimeoutMillis(-1);

    if (tests.contains("refresh")) try {
      setTimeoutMillis(45000);
      startTest("refresh", "Worker refresh (takes 40 seconds)", 5);

      /* Wait for the list to be empty, and verify that it is */

      Thread.sleep(20000);

      Socket s = openSocket(8000);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our initial GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("0\n"))
        testFailed("The server did return a 200 status code to our initial GET /workers, but it was supposed to return '0<LF>', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      /* Register a bunch of workers */

      Random rand = new Random();
      int num = 4+rand.nextInt(3);

      List<String> workers = new LinkedList<String>();
      for (int i=0; i<num; i++) {
        String workerID = randomAlphaNum(5, 7);
        int port = 1024 + rand.nextInt(15000);

        s = openSocket(8000);
        out = new PrintWriter(s.getOutputStream());
        out.print("GET /ping?id="+workerID+"&port="+port+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response while we were registering the "+(i+1)+".th worker ("+workerID+":"+port+"), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code to our worker registration, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();

        workers.add(workerID+":"+port);
      }
 
      Thread.sleep(10000);

      for (String st : workers) {
        String[] pieces = st.split(":");

        s = openSocket(8000);
        out = new PrintWriter(s.getOutputStream());
        out.print("GET /ping?id="+pieces[0]+"&port="+pieces[1]+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response while we were refreshing worker "+st+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code to our worker refresh, but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();
      }

      /* Wait for the entries to expire if they haven't been properly refreshed, and verify that they are still there */

      Thread.sleep(10000);

      s = openSocket(8000);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /workers HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to the second GET /workers, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().startsWith(num+"\n"))
        testFailed("The server did return a 200 status code to the second GET /workers, but it was supposed to return something "+num+" in the first line, since we've registered and then refreshed that many workers, but it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    setTimeoutMillis(-1);


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
      System.out.println("HW4 Coordinator autograder v1.2a (Aug 15, 2023)");
      System.exit(1);
    }

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto")) {
      tests.add("ping");
      tests.add("workers");
      tests.add("table");
      tests.add("expire");
      tests.add("refresh");
    }

    for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup")  && !args[i].equals("cleanup"))
        tests.add(args[i]);

    HW4CoordinatorTest t = new HW4CoordinatorTest();
    t.setExitUponFailure(exitUponFailure);
    if (outputToFile)
      t.outputToFile();
    if (promptUser)
      t.prompt();
    if (runTests)
      t.runTests(tests);
  }
} 
