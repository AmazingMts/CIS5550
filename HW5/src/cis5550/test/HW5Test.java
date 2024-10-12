package cis5550.test;

import java.util.*;
import java.io.*;
import java.net.*;

public class HW5Test extends GenericTest {

  void runSetup() throws Exception {
    File f = new File("__worker");
    if (!f.exists())
      f.mkdir();

    String xtable = "pt-"+randomAlphaNum(4,6);
    String xrow = randomAlphaNum(4,5);
    String xcolumn = randomAlphaNum(4,9);
    String xvalue = randomAlphaNum(8,10);

    f = new File("__worker"+File.separator+xtable);
    if (!f.exists())
      f.mkdir();

    PrintWriter dataOut = new PrintWriter("__worker"+File.separator+xtable+File.separator+xrow);
    dataOut.print(xrow+" "+xcolumn+" "+xvalue.length()+" "+xvalue+" ");
    dataOut.close();

    PrintWriter cfgOut = new PrintWriter("__worker"+File.separator+"config");
    cfgOut.println(xtable);
    cfgOut.println(xrow);
    cfgOut.println(xcolumn);
    cfgOut.println(xvalue);
    cfgOut.close();
  }

  void prompt(Set<String> tests) {
    File f = new File("__worker");

    /* Ask the user to confirm that the server is running */

    System.out.println("In two separate terminal windows, please run:");
    System.out.println("* java cis5550.kvs.Coordinator 8000");
    System.out.println("* java cis5550.kvs.Worker 8001 "+f.getAbsolutePath()+" localhost:8000");
    System.out.println("and then hit Enter in this window to continue. If the Coordinator and/or Worker nodes are already running, please terminate them and then restart them; the test suite has created some files in "+f.getAbsolutePath()+" that are part of the test and will only be read during startup.");
    (new Scanner(System.in)).nextLine();
  }

  void cleanup() throws Exception
  {
    File idf = new File("__worker"+File.separator+"id");
    if (idf.exists())
      idf.delete();

    File d = new File("__worker");
    d.delete();
  }

  void runTests(Set<String> tests) throws Exception {
    File cfg = new File("__worker"+File.separator+"config");
    if (!cfg.exists()) {
      System.err.println("Config file "+cfg.getAbsolutePath()+" does not exist");
      System.exit(1);
    }
    BufferedReader bfr = new BufferedReader(new FileReader(cfg));
    String ytable = bfr.readLine();
    String yrow = bfr.readLine();
    String ycol = bfr.readLine();
    String yvalue = bfr.readLine();
    bfr.close();

    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("wdisk")) try {
      startTest("wdisk", "Writing to disk", 10);
      String row = randomAlphaNum(4,5);
      String col = randomAlphaNum(3,5);
      String val1 = randomAlphaNum(3,5);
      String val2 = randomAlphaNum(3,5);
      String thetable = "pt-"+randomAlphaNum(4,6);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String requestStr = "PUT /data/"+thetable+"/"+row+"/"+col+" HTTP/1.1";
      out.print(requestStr+"\r\nContent-Length: "+val1.length()+"\r\nHost: localhost:8001\r\n\r\n"+val1);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our first PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our first PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      File f = new File("__worker"+File.separator+thetable+File.separator+row);
      if (!f.exists())
        testFailed("We did a "+requestStr+", which should have created a file called '"+f.getAbsolutePath()+"', but this file doesn't seem to exist.");
      FileInputStream fi = new FileInputStream(f);
      byte[] b = new byte[(int)(f.length())];
      fi.read(b);
      String expected1 = row+" "+col+" "+val1.length()+" "+val1+" ";
      String actual = new String(b);
      if (!expected1.equals(actual))
        testFailed("In '"+f.getAbsolutePath()+"', we expected to find:\n\n"+dump(expected1.getBytes())+"\nbut we actually found:\n\n"+dump(b));

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print(requestStr+"\r\nContent-Length: "+val2.length()+"\r\nHost: localhost:8001\r\n\r\n"+val2);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our second PUT ("+requestStr+", with a different body than the first PUT), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our second PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      if (!f.exists())
        testFailed("We did a second "+requestStr+" with a different value. The first created a file called '"+f.getAbsolutePath()+"', but this file doesn't seem to exist anymore after the second request?!?");
      fi = new FileInputStream(f);
      b = new byte[(int)(f.length())];
      fi.read(b);
      String expected2 = row+" "+col+" "+val2.length()+" "+val2+" ";
      actual = new String(b);
      if (expected1.equals(actual))
        testFailed("We did two PUTs to the same row, with different values in the same column. In '"+f.getAbsolutePath()+"', we expected to find:\n\n"+dump(expected2.getBytes())+"\nbut we actually found:\n\n"+dump(b)+"\nwhich is what should have been written after the first PUT. Looks like the file wasn't updated correctly by the second PUT to the same column? Keep in mind that you need to overwrite values instead of appending them!");
      if (!expected2.equals(actual))
        testFailed("We did two PUTs to the same row, with different values in the same column. In '"+f.getAbsolutePath()+"', we expected to find:\n\n"+dump(expected2.getBytes())+"\nbut we actually found:\n\n"+dump(b));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("rdisk")) try {
      startTest("rdisk", "Reading from disk", 5);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String requestStr = "GET /data/"+ytable+"/"+yrow+"/"+ycol+" HTTP/1.1";
      out.print(requestStr+"\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our "+requestStr+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals(yvalue))
        testFailed("We wrote a file called __worker/"+ytable+"/"+yrow+" to disk that contained a row with a single column ("+ycol+") that had value '"+yvalue+"'; then we tried to GET that value. The worker did return a 200 status code to our "+requestStr+", but the value wasn't '"+yvalue+"' as we had expected. Instead, we got:\n\n"+dump(r.body));
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("putget2")) try {
      startTest("putget2", "PUT persistent value, then GET it back", 5);

      String xtable = "pt-"+randomAlphaNum(4,6);
      String xrow = randomAlphaNum(4,5);
      String xcol = randomAlphaNum(5,8);
      String xval = randomAlphaNum(5,8);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String cell = "/data/"+xtable+"/"+xrow+"/"+xcol;
      String req = "PUT "+cell;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\nContent-Length: "+xval.length()+"\r\n\r\n"+xval);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      req = "GET "+cell;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals(xval))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return the string we had PUT in earlier ("+xval+"), and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      String xval2 = randomAlphaNum(5,8);
      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      req = "PUT "+cell;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\nContent-Length: "+xval2.length()+"\r\n\r\n"+xval2);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our second "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The server did return a 200 status code to our second "+req+", but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      req = "GET "+cell;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.body().equals(xval))
        testFailed("The server did return a 200 status code to our "+req+", but it returned the original string we PUT in ("+xval+") instead of the one we PUT in after that ("+xval+"). Check whether you are updating the file offsets in memory correctly.");
      if (!r.body().equals(xval2))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return the second string we had PUT in ("+xval2+"), and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("delete")) try {
      startTest("delete", "Deleting a table", 5);

      String table1 = randomAlphaNum(4,6);
      String table2 = "pt-"+randomAlphaNum(4,6);
      String row1a = randomAlphaNum(4,5);
      String col1a = randomAlphaNum(6,8);
      String val1a = randomAlphaNum(6,8);
      String row2a = randomAlphaNum(4,5);
      String col2a = randomAlphaNum(6,8);
      String val2a = randomAlphaNum(6,8);
      String row2b = randomAlphaNum(4,5);
      String col2b = randomAlphaNum(6,8);
      String val2b = randomAlphaNum(6,8);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());

      out.print("PUT /data/"+table1+"/"+row1a+"/"+col1a+" HTTP/1.1\r\nHost: localhost:8001\r\nContent-Length: "+val1a.length()+"\r\n\r\n"+val1a);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to PUT three values to two different tables, one persistent and one not. The worker returned a "+r.statusCode+" response to our first PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));

      out.print("PUT /data/"+table2+"/"+row2a+"/"+col2a+" HTTP/1.1\r\nHost: localhost:8001\r\nContent-Length: "+val2a.length()+"\r\n\r\n"+val2a);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to PUT three values to two different tables, one persistent and one not. The worker returned a "+r.statusCode+" response to our second PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));

      out.print("PUT /data/"+table2+"/"+row2b+"/"+col2b+" HTTP/1.1\r\nHost: localhost:8001\r\nContent-Length: "+val2b.length()+"\r\n\r\n"+val2b);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to PUT three values to two different tables, one persistent and one not. The worker returned a "+r.statusCode+" response to our third PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      File f = new File("__worker"+File.separator+table2+File.separator+row2a);
      if (!f.exists())
        testFailed("We created a persistent table called '"+table2+"' and PUT a row '"+row2a+"' into it, but there is no file called "+f.getAbsolutePath()); 
      f = new File("__worker"+File.separator+table2+File.separator+row2b);
      if (!f.exists())
        testFailed("We created a persistent table called '"+table2+"' and PUT a row '"+row2b+"' into it, but there is no file called "+f.getAbsolutePath()); 

      /* Check whether we can delete tables */

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /data/"+table1+"/"+row1a+" HTTP/1.1\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to GET value that we had PUT into a table now, but the worker returned a "+r.statusCode+" response, when it should have returned a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("PUT /delete/"+table1+" HTTP/1.1\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to delete an in-memory table we had created earlier, but the worker returned a "+r.statusCode+" response, when it should have returned a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /data/"+table1+"/"+row1a+" HTTP/1.1\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 404)
        testFailed("We were trying to GET value that we had DELETEd just now, but the worker returned a "+r.statusCode+" response, when it should have returned a 404 Not Found. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("PUT /delete/mblfffz HTTP/1.1\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 404)
        testFailed("We were trying to delete a nonexistent table, but the worker returned a "+r.statusCode+" response, when it should have returned a 404 Not Found. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("PUT /delete/"+table2+" HTTP/1.1\r\nHost: localhost:8001\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("We were trying to delete a persistent table we had created earlier, but the worker returned a "+r.statusCode+" response, when it should have returned a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      File idf = new File("__worker"+File.separator+table2+File.separator+row2a);
      if (idf.exists())
        testFailed("We created a row '"+row2a+"' in persistent table '"+table2+"' and then deleted this table, but the file "+idf.getAbsolutePath()+" still exists - it should have been deleted.");
      idf = new File("__worker"+File.separator+table2+File.separator+row2b);
      if (idf.exists())
        testFailed("We created a row '"+row2b+"' in persistent table '"+table2+"' and then deleted this table, but the file "+idf.getAbsolutePath()+" still exists - it should have been deleted.");

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("tablist")) try {
      startTest("tablist", "List of tables", 5);

      String thetable = randomAlphaNum(4,6);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+randomAlphaNum(4,6)+"/"+randomAlphaNum(4,5)+" HTTP/1.1\r\nContent-Length: 3\r\nHost: localhost\r\n\r\nFoo");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response for our GET /, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("content-type") == null)
        testFailed("In response to our GET /, the worker did NOT return a Content-Type: header");
      if (!r.headers.get("content-type").equals("text/html"))
        testFailed("In response to our GET /, the worker did return a Content-Type: header, but its value was '"+r.headers.get("content-type")+"', when we expected text/html.");
      if (!r.body().toLowerCase().contains("<html>"))
        testFailed("In response to our GET /, the worker did return text/html content, but we couldn't find a <html> tag. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().toLowerCase().contains("<table"))
        testFailed("In response to our GET /, the worker did return a HTML page, but we couldn't find a <table> tag. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().contains(thetable))
        testFailed("In the HTML page the worker sent for our GET /, there should have been an entry for table '"+thetable+"', but there wasn't. Here is what was in the body:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("tabview")) try {
      startTest("tabview", "Table view", 5);

      String thetable = randomAlphaNum(4,6);
      String therow = randomAlphaNum(4,5);
      String thecolumn = randomAlphaNum(4,6);
      String thevalue = randomAlphaNum(4,6);

      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+therow+"/"+thecolumn+" HTTP/1.1\r\nContent-Length: "+thevalue.length()+"\r\nHost: localhost\r\n\r\n"+thevalue);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /view/"+thetable+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response for our GET /view/"+thetable+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (r.headers.get("content-type") == null)
        testFailed("In response to our GET /view/"+thetable+", the worker did NOT return a Content-Type: header");
      if (!r.headers.get("content-type").equals("text/html"))
        testFailed("In response to our GET /view/"+thetable+", the worker did return a Content-Type: header, but its value was '"+r.headers.get("content-type")+"', when we expected text/html.");
      if (!r.body().toLowerCase().contains("<html>"))
        testFailed("In response to our GET /view/"+thetable+", the worker did return text/html content, but we couldn't find a <html> tag. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().toLowerCase().contains("<table"))
        testFailed("In response to our GET /view/"+thetable+", the worker did return a HTML page, but we couldn't find a <table> tag. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().contains(thetable))
        testFailed("In the HTML page the worker sent for our GET /view/"+thetable+", table '"+thetable+"' should have been mentioned, but it wasn't. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().contains(therow))
        testFailed("In the HTML page the worker sent for our GET /view/"+thetable+", there should have been an entry for row '"+therow+"', but there wasn't. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().contains(thecolumn))
        testFailed("In the HTML page the worker sent for our GET /view/"+thetable+", there should have been an entry for column '"+thecolumn+"', but there wasn't. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().contains(thevalue))
        testFailed("In the HTML page the worker sent for our GET /view/"+thetable+", there should have been an entry for value '"+thevalue+"', but there wasn't. Here is what was in the body:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("readrow")) try {
      startTest("readrow", "Whole-row read", 5);

      String thetable = randomAlphaNum(4,6);
      String row = randomAlphaNum(4,5);
      String col1 = randomAlphaNum(3,5);
      String col2 = randomAlphaNum(3,5);
      String val1 = randomAlphaNum(3,5);
      String val2 = randomAlphaNum(3,5);
 
      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+row+"/"+col1+" HTTP/1.1\r\nContent-Length: "+val1.length()+"\r\nHost: localhost\r\n\r\n"+val1);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our first PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our first PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+row+"/"+col2+" HTTP/1.1\r\nContent-Length: "+val2.length()+"\r\nHost: localhost\r\n\r\n"+val2);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our second PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our second PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /data/"+thetable+"/"+row+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response for our GET /data/"+thetable+"/"+row+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      String option1 = row+" "+col1+" "+val1.length()+" "+val1+" "+col2+" "+val2.length()+" "+val2+" ";
      String option2 = row+" "+col2+" "+val2.length()+" "+val2+" "+col1+" "+val1.length()+" "+val1+" ";
      if (!r.body().equals(option1) && !r.body().equals(option2))
        testFailed("In the response to our whole-row GET /"+thetable+"/"+row+", we expected to see one of the following:\n\n  * "+option1+"\n  * "+option2+"\n\nbut we didn't. Here is what was in the body instead:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("rstream")) try {
      startTest("rstream", "Streaming read", 10);

      String thetable = randomAlphaNum(4,6);
      String row1 = randomAlphaNum(4,5);
      String row2 = randomAlphaNum(4,5);
      String col1 = randomAlphaNum(3,5);
      String col2 = randomAlphaNum(3,5);
      String val1 = randomAlphaNum(3,5);
      String val2 = randomAlphaNum(3,5);
 
      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+row1+"/"+col1+" HTTP/1.1\r\nContent-Length: "+val1.length()+"\r\nHost: localhost\r\n\r\n"+val1);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our first PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our first PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("PUT /data/"+thetable+"/"+row2+"/"+col2+" HTTP/1.1\r\nContent-Length: "+val2.length()+"\r\nHost: localhost\r\n\r\n"+val2);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response to our second PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The worker did return a 200 status code to our second PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      out.print("GET /data/"+thetable+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response for our GET /data/"+thetable+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      String option1 = row1+" "+col1+" "+val1.length()+" "+val1+" \n"+row2+" "+col2+" "+val2.length()+" "+val2+" \n\n";
      String option2 = row2+" "+col2+" "+val2.length()+" "+val2+" \n"+row1+" "+col1+" "+val1.length()+" "+val1+" \n\n";
      if (!r.body().equals(option1) && !r.body().equals(option2))
        testFailed("In the response to our streaming GET /"+thetable+", we expected to see one of the following:\n\n"+dump(option1.getBytes())+"\n"+dump(option2.getBytes())+"\nbut we didn't. Here is what was in the body instead:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("rename")) try {
      startTest("rename", "Rename a table", 5);
      Socket s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      String thetable = randomAlphaNum(4,6);
      String therow = randomAlphaNum(4,5);
      String thecol = randomAlphaNum(5,8);
      String cell = "/data/"+thetable+"/"+therow+"/"+thecol;
      String thedata = randomAlphaNum(10,20);
      String req = "PUT "+cell;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\nContent-Length: "+thedata.length()+"\r\n\r\n"+thedata);
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      String newtable = randomAlphaNum(4,6);
      req = "PUT /rename/"+thetable;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\nContent-Length: "+newtable.length()+"\r\n\r\n"+newtable);
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals("OK"))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      req = "GET /data/"+newtable+"/"+therow+"/"+thecol;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals(thedata))
        testFailed("The server did return a 200 status code to our "+req+", but it was supposed to return the string we had PUT in earlier ("+thedata+"), and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
      s.close();

      s = openSocket(8001);
      out = new PrintWriter(s.getOutputStream());
      req = "GET /data/"+thetable+"/"+therow+"/"+thecol;
      out.print(req+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      r = readAndCheckResponse(s, "response");
      if (r.statusCode != 404)
        testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 404 Not Found. Here is what was in the body:\n\n"+dump(r.body));
      s.close();

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("count")) try {
      startTest("count", "Row count", 5);

      String thetable = randomAlphaNum(4,6);
 
      Socket s;;
      int num = 5+(new Random()).nextInt(10);
      for (int i=0; i<num; i++) {
        s = openSocket(8001);
        PrintWriter out = new PrintWriter(s.getOutputStream());
        out.print("PUT /data/"+thetable+"/"+randomAlphaNum(4,5)+"/"+randomAlphaNum(12,15)+" HTTP/1.1\r\nContent-Length: "+8+"\r\nHost: localhost\r\n\r\n"+randomAlphaNum(8,8));
        out.flush();
        Response r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The worker returned a "+r.statusCode+" response to our "+(i+1)+".th PUT, but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The worker did return a 200 status code to our "+(i+1)+".th PUT, but it was supposed to return 'OK', and it didn't. Here is what was in the body:\n\n"+dump(r.body));
        s.close();
      }

      s = openSocket(8001);
      PrintWriter out = new PrintWriter(s.getOutputStream());
      out.print("GET /count/"+thetable+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
      out.flush();
      Response r = readAndCheckResponse(s, "response");
      if (r.statusCode != 200)
        testFailed("The worker returned a "+r.statusCode+" response for our GET /count/"+thetable+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
      if (!r.body().equals(""+num))
        testFailed("In the response to our GET /count/"+thetable+", we expected to see '"+num+"', but we didn't. Here is what was in the body instead:\n\n"+dump(r.body));

      testSucceeded();
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("pages")) try {
      startTest("pages", "Paginated user interface", 5);
      String thetable = randomAlphaNum(4,6);
      int num = 50+(new Random()).nextInt(100);
      for (int i=0; i<num; i++) {
        Socket s = openSocket(8001);
        PrintWriter out = new PrintWriter(s.getOutputStream());
        String therow = randomAlphaNum(4,5);
        String thecol = randomAlphaNum(5,8);
        String cell = "/data/"+thetable+"/"+therow+"/"+thecol;
        String thedata = randomAlphaNum(10,20);
        String req = "PUT "+cell;
        out.print(req+" HTTP/1.1\r\nHost: localhost\r\nContent-Length: "+thedata.length()+"\r\n\r\n"+thedata);
        out.flush();
        Response r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response to our "+req+" (our "+(i+1)+".th request), but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().equals("OK"))
          testFailed("The server did return a 200 status code to our "+req+" (our "+(i+1)+".th request), but it was supposed to return 'OK', and it didn't. Here is what was in the body instead:\n\n"+dump(r.body));
        s.close();
      }

      String initialurl = "/view/"+thetable, url = initialurl;
      int iterations = 0;
      while (iterations < 100) {
        iterations ++;
        Socket s = openSocket(8001);
        PrintWriter out = new PrintWriter(s.getOutputStream());
        String req = "GET "+url;
        out.print(req+" HTTP/1.1\r\nHost: localhost\r\n\r\n");
        out.flush();
        Response r = readAndCheckResponse(s, "response");
        if (r.statusCode != 200)
          testFailed("The server returned a "+r.statusCode+" response to our "+req+", but we were expecting a 200 OK. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.headers.get("content-type").equals("text/html"))
          testFailed("In response to our "+req+", the worker did return a Content-Type: header, but its value was '"+r.headers.get("content-type")+"', when we expected text/html.");
        if (!r.body().toLowerCase().contains("<html>"))
          testFailed("In response to our "+req+", the worker did return text/html content, but we couldn't find a <html> tag. Here is what was in the body:\n\n"+dump(r.body));
        if (!r.body().toLowerCase().contains("<table"))
          testFailed("In response to our "+req+", the worker did return a HTML page, but we couldn't find a <table> tag. Here is what was in the body:\n\n"+dump(r.body));
        int pos = r.body().indexOf("a href=\"");
        if (pos < 0) {
          pos = r.body().indexOf("a href='");
          if (pos < 0) {
            pos = r.body().indexOf("a href=");
            if (pos < 0)
              break;
            url = (r.body().substring(pos+7)).split("[ >]")[0];
          }
          else {
            url = (r.body().substring(pos+8)).split("'")[0];
          }
        } else {
          url = (r.body().substring(pos+8)).split("\"")[0];
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
          url = url.substring(url.startsWith("http://") ? 7 : 8);
          url = url.substring(url.indexOf("/"));
        }
        s.close();
      }

      int expected = (num/10);
      if (num > expected*10)
        expected ++;

      if (iterations < expected)
        testFailed("We uploaded "+num+" rows to table '"+thetable+"', but we were only able to find "+iterations+" pages, starting from "+initialurl+". With 10 rows per page, there should have been "+expected+" pages.");
      if (iterations > expected)
        testFailed("We uploaded "+num+" rows to table '"+thetable+"', but we found at least "+iterations+" pages, starting from "+initialurl+". With 10 rows per page, there should only have been "+expected+".");

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
    boolean runSetup = true, runTests = true, promptUser = true, outputToFile = false, exitUponFailure = true, cleanup = true;

    if ((args.length > 0) && args[0].equals("auto")) {
      runSetup = false;
      runTests = true;
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
      System.out.println("HW5 autograder v1.2 (Oct 1, 2023)");
      System.exit(1);
    }

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto")) {
      tests.add("wdisk");
      tests.add("rdisk");
      tests.add("putget2"); 
      tests.add("delete"); 
      tests.add("tablist");
      tests.add("tabview");
      tests.add("readrow");
      tests.add("rstream");
      tests.add("rename");
      tests.add("count");
      tests.add("pages");
      tests.add("persist");
    } 

    for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup") && !args[i].equals("cleanup")) 
    		tests.add(args[i]);

    HW5Test t = new HW5Test();
    t.setExitUponFailure(exitUponFailure);
    if (outputToFile)
      t.outputToFile();
    if (runSetup)
      t.runSetup();
    if (promptUser)
      t.prompt(tests);
    if (runTests)
      t.runTests(tests);
    if (cleanup)
      t.cleanup();
  }
} 
