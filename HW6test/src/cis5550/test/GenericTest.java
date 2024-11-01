package cis5550.test;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;

class GenericTest {
  boolean exitUponFailure;
  boolean outputToFile;
  boolean isFirstTest;
  PrintWriter outputFile;
  boolean currentTestFailed;
  int numTestsFailed;
  boolean isExtraCredit;
  Thread timeoutThread;
  long timeoutMillis;
  boolean isTimeout;
  int maxPoints;

  class Counter {
    int value;
    Counter(int valueArg) {
      value = valueArg;
    }
    synchronized boolean decrement() {
      return (value--) > 0;
    }
    synchronized void increment() {
      value ++;
    }
    synchronized boolean aboveZero() {
      return (value>0);
    }
    synchronized int getValue() {
      return value;
    }
  };

  class Response {
    int statusCode;
    HashMap<String,String> headers;
    byte body[], response[];

    Response() {
      statusCode = 0;
      headers = new HashMap<String,String>();
      body = null;
      response = null;
    }

    String body() {
      return new String(body, StandardCharsets.UTF_8);
    }

    byte[] bodyAsBytes() {
      return body;
    }
  };

  GenericTest() {
    exitUponFailure = true;
    outputToFile = false;
    isFirstTest = true;
    outputFile = null;
    currentTestFailed = false;
    numTestsFailed = 0;
    timeoutMillis = 5000;
  }

  void setExitUponFailure(boolean arg) {
    exitUponFailure = arg;
  }

  void outputToFile() throws IOException {
    outputToFile = true;
    outputFile = new PrintWriter("test_results.json");
    outputFile.println("{");
  }

  void closeOutputFile() {
    if (outputToFile) {
      outputFile.println();
      outputFile.println("}");
      outputFile.close();
    }
  }

  void setTimeoutMillis(long millisOrMinus1) {
    timeoutMillis = (millisOrMinus1>0) ? millisOrMinus1 : 5000;
  }

  void startTest(String testCode, String testName, int maxPointsArg) {
    startTest(testCode, testName, maxPointsArg, false);
  }

  void startTest(String testCode, String testName, int maxPointsArg, boolean isExtraCreditArg) {
    final Thread mainThread = Thread.currentThread();
    isTimeout = false;
    timeoutThread = new Thread("Timeout") { public void run() {
      try { Thread.sleep(timeoutMillis); } catch (InterruptedException ie) { return; }
//      System.err.println("*** TIMEOUT *** ("+timeoutMillis+" ms)");
      isTimeout = true;
      mainThread.interrupt();
    } };
    timeoutThread.start();

    currentTestFailed = false;
    isExtraCredit = isExtraCreditArg;
    System.out.printf("%-9s %-40s", testCode, testName);
    if (outputToFile) {
      if (!isFirstTest)
        outputFile.println(",");
      outputFile.print("  \""+testCode+"\": { ");
      maxPoints = maxPointsArg;
    }
  }

  synchronized void testSucceeded() {
    if (currentTestFailed)
      return;

    timeoutThread.interrupt();
    try { timeoutThread.join(); } catch (InterruptedException ie) {}

    isFirstTest = false;
    System.out.println("[ OK ]");
    if (outputToFile)
      outputFile.print("\"testStatus\": \"PASS\", \"testComments\": \"\", \"maxPoints\": "+(isExtraCredit ? 0 : maxPoints)+", \"points\": "+maxPoints+" }");
  }

  synchronized void testFailed(String explanation) throws Exception {
    testFailed(explanation, false);
  }

  synchronized void testFailed(String explanation, boolean throwException) throws Exception {
    if (!currentTestFailed) {
      numTestsFailed ++;
      timeoutThread.interrupt();
      while (true) { try { timeoutThread.join(); break; } catch (InterruptedException ie) {} }
      System.out.println("[FAIL]");
      if (outputToFile) {
        outputFile.print("\"testStatus\": \"FAIL\", \"testComments\": \""+explanation.replace("\"", "\\\"").replace("\n","\\n")+"\", \"maxPoints\": "+(isExtraCredit ? 0 : maxPoints)+", \"points\": 0 }");
System.out.println("\n"+explanation);
      } else {
        System.out.println("\n"+explanation);
      }
    }
    currentTestFailed = true;
    if (exitUponFailure) {
      closeOutputFile();
      System.exit(1);
    }
    isFirstTest = false;
    if (throwException)
      throw new Exception("Test failed");
  }

  String randomAlphaNum(int minLen, int maxLen) {
    Random x = new Random();
    int len = minLen+x.nextInt(maxLen-minLen+1);
    String theVal = "";
    for (int i=0; i<len; i++)
      theVal = theVal + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(x.nextInt(62));
    return theVal;
  }

  int random(int min, int max) {
    Random x = new Random();
    return min+x.nextInt(max-min+1);
  }

  boolean isPrint(int b)
  {
    if (Character.isLetter(b) || Character.isDigit(b))
      return true;
    if ((b == ' ') || (b == '=') || (b == '/') || (b == '-') || (b == ':') || (b == '<') || (b == '>') || (b == '(') || (b == ')') || (b == '!') || (b == ',') || (b == '+') || (b == '%') || (b == '\"') || (b == '[') || (b == ']'))
      return true;

    return false;
  }

  String dump(byte[] data)
  {
    String result = "------------------------------------------------------------------------\n";
    if (data != null) {
      for (int i=0; i<data.length; i+=16) {
        result += String.format("%04x  ", i);
        for (int j=0; j<16; j++) {
          if ((i+j)<data.length)
            result += String.format("%02x ", data[i+j]);
          else
            result += "   ";
        }
        result += "|";
        for (int j=0; j<16; j++) {
          if ((i+j)<data.length)
            result += isPrint(data[i+j]) ? String.format("%c", data[i+j]) : ".";
          else
            result += " ";
        }
        result += "|\n";
      }
    } else {
      result = result + "(no data)\n";
    }
    return result + "------------------------------------------------------------------------\n";
  }

  void assertClosed(Socket s, String explanationIfTimeout, String explanationIfMoreData) throws Exception {
    int MAXIMUM_DUMP_SIZE = 600;
    InputStream in = null;
    try {
      in = s.getInputStream();
      s.setSoTimeout(1000);
    } catch (Exception e) {
      testFailed("An unknown problem occurred when reading: "+e);
    }
    int b = 0;
    try {
      b = in.read();
    } catch (SocketTimeoutException ste) {
      testFailed(explanationIfTimeout);
      return;
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    if (b != -1) {
      long startTimeMs = System.currentTimeMillis();
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      buffer.write(b);
      try {
        while (buffer.size()<MAXIMUM_DUMP_SIZE) {
          b = in.read();
          if ((System.currentTimeMillis()-startTimeMs)>2000) {
            testFailed("Reading the extra data from the server took more than 2 seconds; aborting this test.");
            return;
          }
          if (b == -1)
            break;
          buffer.write(b);
        }
      } catch (Exception e) {};
      testFailed(explanationIfMoreData+"\n\nHere is the extra data "+((buffer.size()>=MAXIMUM_DUMP_SIZE) ? "(the first "+MAXIMUM_DUMP_SIZE+" bytes) " : "")+"we got:\n\n"+dump(buffer.toByteArray()));
    }
  }

  void assertNoResponseYet(Socket s, String explanationIfResponse) throws Exception  {
    InputStream in = null;
    try {
      in = s.getInputStream();
      s.setSoTimeout(200);
    } catch (Exception e) {
      testFailed("An unknown problem occurred when reading: "+e);
    }
    int b = 0;
    boolean gotSTE = false;
    try {
      b = in.read();
    } catch (SocketTimeoutException ste) {
      gotSTE = true;
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    if (!gotSTE) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      buffer.write(b);
      try {
        while (true) {
          b = in.read();
          if (b == -1)
            break;
          buffer.write(b);
        }
      } catch (Exception e) {};
      testFailed(explanationIfResponse+"\n\nHere is the data we got:\n\n"+dump(buffer.toByteArray()));
    }
  }

  Response readAndCheckResponse(Socket s, String which) throws Exception  {
    return readAndCheckResponse(s, which, true, false);
  }

  Response readAndCheckResponse(Socket s, String which, boolean expectContentLength) throws Exception  {
    return readAndCheckResponse(s, which, expectContentLength, false);
  }

  Response readAndCheckResponse(Socket s, String which, boolean expectContentLength, boolean isHEAD) throws Exception  {
    Response r = new Response();
    InputStream in = null;
    long startTimeMs = System.currentTimeMillis();
    try {
      in = s.getInputStream();
      s.setSoTimeout(1000);
    } catch (Exception e) {
      testFailed("An unknown problem occurred when reading: "+e);
    }
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    ByteArrayOutputStream entire = new ByteArrayOutputStream();
    int matchPtr = 0, numBytesRead = 0;
    while (matchPtr < 4) {
      int b = 0;
      if (isTimeout) {
        testFailed("This test timed out after "+timeoutMillis+"ms");
        throw new Exception("Test failed due to timeout");
      }
      try {
        b = in.read();
        if (b>=0)
          numBytesRead ++;
      } catch (SocketTimeoutException ste) {
        if (numBytesRead == 0)
          testFailed("A timeout occurred before any (!) bytes were received from the server.");
        testFailed("A timeout occurred before the entire "+which+" could be read. Here is what we received so far:\n\n"+dump(buffer.toByteArray())+"\nCheck whether you are sending two (!) CRLFs at the end of the headers (not just one, and not just a CR), and whether you need to flush the OutputStream.");
        return null;
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

      if (b<0) {
        if (numBytesRead == 0)
          testFailed("The server closed the connection before any (!) bytes of the "+which+" were received.");
        testFailed("The server closed the connection before the entire "+which+" could be read. Here is what we received so far:\n\n"+dump(buffer.toByteArray())+"\nCheck whether you are sending two (!) CRLFs at the end of the headers (not just one, and not just a CR), and whether you need to flush the OutputStream.");
        return null;
      }

      buffer.write(b);
      entire.write(b);
      if ((((matchPtr==0) || (matchPtr==2)) && (b=='\r')) || (((matchPtr==1) || (matchPtr==3)) && (b=='\n')))
        matchPtr ++;
      else
        matchPtr = 0;
    }

    try {
      BufferedReader hdr = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.toByteArray())));
      String statusLine = hdr.readLine();
      String[] p = statusLine.split(" ");
      if (p.length < 3) {
        testFailed("The status line we got was '"+statusLine+"'. It is supposed to have 3 fields, but it looks like it has only "+p.length+". Here is what we received:\n\n"+dump(buffer.toByteArray()));
        return null;
      }

      if (!p[0].equals("HTTP/1.1")) {
        testFailed("The status line we got was '"+statusLine+"'. The protocol was '"+p[0]+"', but it was supposed to be HTTP/1.1. Here is what we received:\n\n"+dump(buffer.toByteArray()));
        return null;
      }
      if (p[1].length() != 3) {
        testFailed("The status line we got was '"+statusLine+"'. The status code, '"+p[1]+"', was supposed to have three digits, but it doesn't. Here is what we received:\n\n"+dump(buffer.toByteArray()));
        return null;
      }

      try {
        r.statusCode = Integer.parseInt(p[1]);
      } catch (NumberFormatException nfe) {
        testFailed("The status line we got was '"+statusLine+"'. The status code, '"+p[1]+"', was supposed to be numeric, but it isn't. Here is what we received:\n\n"+dump(buffer.toByteArray()));
        return null;
      }

      while (true) {
        String l = hdr.readLine();
        if (l.equals(""))
          break;

        String[] p2 = l.split(":", 2);
        if (p2.length == 2) {
          r.headers.put(p2[0].toLowerCase().trim(), p2[1].trim());
        } else {
          testFailed("We got a header line without a colon: '"+l+". Here is what we received:\n\n"+dump(buffer.toByteArray()));
          return null;
        }
      }

    } catch (IOException ioe) {
      testFailed("Unknown exception while parsing the headers: "+ioe);
      ioe.printStackTrace();
      return null;
    }

    if (!isHEAD) {
      ByteArrayOutputStream body = new ByteArrayOutputStream();
      String cl = r.headers.get("content-length");
      int bodyBytesReceived = 0;
      byte buf[] = new byte[1024];
      for (int i=0; (cl == null) || (i<Integer.valueOf(cl).intValue()); ) {
        int nRead = 0;
        try {
          nRead = in.read(buf);
          if (isTimeout) {
            testFailed("Test timed out after "+timeoutMillis+"ms");
            return null;
          }
        } catch (SocketTimeoutException ste) {
          if (expectContentLength)
            testFailed("A timeout occurred before the entire message body could be read. We got "+i+" bytes, but expected "+cl+". "
                      +"Here is what we received so far:\n\n"+dump(buffer.toByteArray())+"\nCheck whether you sent a Content-Length header "
                      +((cl==null) ? "(we did not see one in the response)" : "(the value we saw was: '"+cl+"')")
                      +", and, if so, whether the value in that header is correct. An off-by-one error can easily cause this test to fail. ");
          else
            testFailed("A timeout occurred before the entire message body could be read. We got "+i+" bytes so far, which are:\n\n"+dump(buffer.toByteArray())+"\n"
                      +"Check whether you closed the connection after handling the request.");
          return null; 
        } catch (IOException ioe) {
          ioe.printStackTrace();
        } catch (InterruptedException ie) {
          if (isTimeout) {
            testFailed("Test timed out after "+timeoutMillis+"ms");
            return null;
          }
        }
        if (nRead<0) {
          if (cl != null) {
            testFailed("The server closed the connection before the entire message body could be read. Here is what we received so far:\n\n"+dump(buffer.toByteArray())+"\nCheck whether you sent a Content-Length header (the value we saw was: '"+cl+"'), and, if so, whether the value in that header is correct. An off-by-one error can easily cause this test to fail.");
            return null;
          } else {
            break;
          }
        }

        i += nRead;

        body.write(buf, 0, nRead);
        entire.write(buf, 0, nRead);
      }

      r.body = body.toByteArray();
    }
    
    r.response = entire.toByteArray();
    return r;
  }

  Socket openSocket(int port) throws Exception {
    Socket s = null;
    try {
      s = new Socket("localhost", port);
    } catch (ConnectException ce) {
      testFailed("Cannot connect to port "+port+" on this machine. Are you sure the server is running and has port "+port+" open? You can use 'ps' and 'netstat' to check.");
    } catch (Exception e) {
      testFailed("Unknown problem: "+e);
      e.printStackTrace();
    }

    return s;
  }
}