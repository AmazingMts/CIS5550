package cis5550.test;

import cis5550.flame.FlameSubmit;
import cis5550.kvs.*;
import cis5550.tools.HTTP;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;

public class HW8Test extends GenericTest {

  void runSetup() {
  }

  void prompt() {

    /* Ask the user to confirm that the server is running */

    System.out.println("Make sure the student's solution is in a file called 'crawler.jar' in the local directory.");
    System.out.println("Then, in separate terminal windows, please run the following commands:");
    System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar cis5550.kvs.Coordinator 8000");
    System.out.println("  rm -rf worker1; java -cp lib/kvs.jar:lib/webserver.jar cis5550.kvs.Worker 8001 worker1 localhost:8000");
    System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar cis5550.flame.Coordinator 9000 localhost:8000");
    System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar cis5550.flame.Worker 9001 localhost:9000");
    System.out.println("... and then hit Enter in this window to continue.");
    (new Scanner(System.in)).nextLine();

  }

  void cleanup() {

  }

  String verify(KVSClient kvs, String refFile) throws Exception {
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(refFile));
    Map<String,Row> data = new HashMap<String,Row>();
    while (true) {
      Row r = Row.readFrom(is);
      if (r == null)
        break;
      data.put(r.get("url"), r);
    }
    is.close();

    Iterator<Row> iter = kvs.scan("pt-crawl", null, null);
    String problems = "";
    while (iter.hasNext()) {
      Row r = iter.next();
      if (r.get("url") != null) {
        if (data.get(r.get("url")) != null) {
          Row ref = data.get(r.get("url"));
          if ((ref.get("responseCode") != null) && ((r.get("responseCode") == null) || !ref.get("responseCode").equals(r.get("responseCode")))) {
            problems = problems + "  * Response code for "+r.get("url")+" is "+r.get("responseCode")+", should be "+ref.get("responseCode")+"\n";
          }
          if ((ref.get("contentType") != null) && ((r.get("contentType") == null) || !ref.get("contentType").equals(r.get("contentType")))) {
            problems = problems + "  * Content type for "+r.get("url")+" is "+r.get("contentType")+", should be "+ref.get("contentType")+"\n";
          }
          if ((ref.get("length") != null) && ((r.get("length") == null) || !ref.get("length").equals(r.get("length")))) {
            problems = problems + "  * Length for "+r.get("url")+" is "+r.get("length")+", should be "+ref.get("length")+"\n";
          }
          if ((ref.get("page") != null) && ((r.get("page") == null) || !ref.get("page").equals(r.get("page")))) {
            problems = problems + "  * Page data for "+r.get("url")+" is not what it should be\n";
          }
          data.remove(r.get("url"));
        } else {
          problems = problems + "  * Extra entry for "+r.get("url")+" (not in reference data)\n";
        }
      } else {
        problems = problems + "  * Row in 'crawl' table with key "+r.key()+" has no 'url' column\n";
      }
    } 
    for (String k : data.keySet()) {
      problems = problems + "  * Crawl table is missing entry for '"+k+"'\n";
    }
    return problems;
  }

  Row findRow(KVSClient kvs, String url) throws Exception {
    Iterator<Row> iter = kvs.scan("pt-crawl", null, null);
    while (iter.hasNext()) {
      Row r = iter.next();
      if ((r.get("url") != null) && r.get("url").equals(url))
        return r;
    }
    return null;
  }

  Row findAnyRowWhoseURLContains(KVSClient kvs, String pattern) throws Exception {
    Iterator<Row> iter = kvs.scan("pt-crawl", null, null);
    while (iter.hasNext()) {
      Row r = iter.next();
      if ((r.get("url") != null) && r.get("url").contains(pattern))
        return r;
    }
    return null;
  }

  void runTests(Set<String> tests) throws Exception {

    KVSClient kvs = new KVSClient("localhost:8000");


    if (tests.contains("run-simple")) {
      System.out.println("Running the crawler on 'simple'...");
      String arg[] = new String[] { "http://simple.crawltest.401.cis5550.net" };
      String output = FlameSubmit.submit("localhost:9000", "crawler.jar", "cis5550.jobs.Crawler", arg);
      if (output == null) 
        System.out.println("Looks like the run failed; the response code was "+FlameSubmit.getResponseCode()+", and the output was:\n\n"+FlameSubmit.getErrorResponse()+"\n\nRunning the tests anyway, but it may be better to look into the reasons for the failure first...\n");
      else 
        System.out.println("Looks like the run succeeded; the output was:\n\n"+output+"\n\nRunning the test cases now...\n");
    }

    if (tests.contains("run-advanced")) {
      System.out.println("Running the crawler on 'advanced'...");
      String arg[] = new String[] { "http://advanced.crawltest.401.cis5550.net" };
      String output = FlameSubmit.submit("localhost:9000", "crawler.jar", "cis5550.jobs.Crawler", arg);
      if (output == null) 
        System.out.println("Looks like the run failed; the response code was "+FlameSubmit.getResponseCode()+", and the output was:\n\n"+FlameSubmit.getErrorResponse()+"\n\nRunning the tests anyway, but it may be better to look into the reasons for the failure first...\n");
      else 
        System.out.println("Looks like the run succeeded; the output was:\n\n"+output+"\n\nRunning the test cases now...\n");
    }

    System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
    System.out.println("--------------------------------------------------------");

    if (tests.contains("simple")) try {
      startTest("simple", "simple.crawltest.401.cis5550.net", 40);
      String problems = verify(kvs, "simple.crawl");
      if (problems.equals(""))
        testSucceeded();
      else
        testFailed("The crawl of simple.crawltest.401.cis5550.net returned incorrect results:\n\n"+problems+"\n@Grader: Give partial credit if appropriate, based on the above differences.");
    } catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }

    if (tests.contains("advanced")) try {
      startTest("advanced", "advanced.crawltest.401.cis5550.net", 25);
      String problems = verify(kvs, "advanced.crawl");
      if (problems.equals(""))
        testSucceeded();
      else
        testFailed("The crawl of advanced.crawltest.401.cis5550.net returned incorrect results:\n\n"+problems+"\n@Grader: Give partial credit if appropriate, based on the above differences.");
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
      System.out.println("HW8 autograder v1.1a (Oct 23, 2023)");
      System.exit(1);
    }

//promptUser = false;  [needed for autograder]

    if ((args.length == 0) || args[0].equals("all") || args[0].equals("auto") || (args.length>1) && (args[1].equals("all") || args[1].equals("auto"))) {
    }

    for (int i=0; i<args.length; i++)
      if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup") && !args[i].equals("cleanup")) 
        tests.add(args[i]);

    HW8Test t = new HW8Test();
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
