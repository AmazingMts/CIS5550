package cis5550.test;

import cis5550.flame.FlameSubmit;
import cis5550.kvs.*;
import cis5550.tools.HTTP;
import cis5550.tools.Hasher;
import cis5550.external.PorterStemmer;
import java.util.*;
import java.io.*;

public class HW9Test extends GenericTest {

	void runSetup() {
	}

	void prompt() {
		/* Ask the user to confirm that the server is running */

		System.out.println(
				"Make sure the student's solutions are in file called 'indexer.jar' and 'pagerank.jar' in the local directory.");
		System.out.println("Then, in separate terminal windows, please run the following commands:");
		System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar cis5550.kvs.Coordinator 8000");
		System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar cis5550.kvs.Worker 8001 worker1 localhost:8000");
		System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar cis5550.flame.Coordinator 9000 localhost:8000");
		System.out.println("  java -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar cis5550.flame.Worker 9001 localhost:9000");
		System.out.println("... and then hit Enter in this window to continue.");
		(new Scanner(System.in)).nextLine();
	}

	void cleanup() {
	}

	void installCrawl(Map<String, String> data) throws IOException {
		KVSClient kvs = new KVSClient("localhost:8000");
		HTTP.Response response = HTTP.doRequest("GET", "http://" + kvs.getWorkerAddress(0) + "/tables", null);
		if (response != null) {
		    String[] listOfTables = new String(response.body()).split("\n");
		    for (String table: listOfTables) {
		        kvs.delete(table);
		    }
		}
		for (String s : data.keySet()) {
			String hash = Hasher.hash(s);
			kvs.put("pt-crawl", hash, "url", s);
			kvs.put("pt-crawl", hash, "page", data.get(s));
		}
	}

	static String[] separate(String s) {
		if (!s.startsWith("http"))
			return s.split(":");
		String pcs[] = s.substring(6).split(":");
		if (pcs.length == 1)
			return new String[] { s };
		return new String[] { s.substring(0, 6) + pcs[0], pcs[1] };
	}

	boolean theSame(String list1, String list2, boolean compareColumns) {
		String l1[] = list1.split(",");
		String l2[] = list2.split(",");
		if (!compareColumns) {
			for (int i = 0; i < l1.length; i++)
				l1[i] = separate(l1[i])[0];
			for (int i = 0; i < l2.length; i++)
				l2[i] = separate(l2[i])[0];
		}

		for (int i = 0; i < l1.length; i++) {
			boolean found = false;
			for (int j = 0; j < l2.length; j++) {
				if (l2[j].equals(l1[i]))
					found = true;
			}
			if (!found) {
				return false;
			}
		}

		for (int i = 0; i < l2.length; i++) {
			boolean found = false;
			for (int j = 0; j < l1.length; j++)
				if (l1[j].equals(l2[i]))
					found = true;
			if (!found) 
				return false;
		}

		return true;
	}

	String compareToExpected(HashMap<String, String> expected, boolean doStem, boolean ignoreWordPos)
			throws FileNotFoundException, IOException {
		HashMap<String, String> extra = new HashMap<String, String>();
		if (doStem) {
			for (String s : expected.keySet()) {
				PorterStemmer stemmer = new PorterStemmer();
				stemmer.add(s.toCharArray(), s.length());
				stemmer.stem();
				extra.put(stemmer.toString(), expected.get(s));
			}
		}

		KVSClient kvs = new KVSClient("localhost:8000");

		Iterator<Row> iter = kvs.scan("pt-index", null, null);
		String problems = "";
		while (iter.hasNext()) {
			Row r = iter.next();
			String col1 = r.columns().iterator().next();
			if (!expected.containsKey(r.key()) && !extra.containsKey(r.key()))
				problems = problems + " * The 'pt-index' table contains a row with key '" + r.key()
						+ "', but this wasn't a word in our input data\n";
			else if (r.columns().size() != 1)
				problems = problems + " * In the 'pt-index' table, the row with key '" + r.key() + "' has "
						+ r.columns().size() + " columns, but it should only have one\n";
			else if (expected.containsKey(r.key()) && !theSame(r.get(col1), expected.get(r.key()), !ignoreWordPos)) {
				problems = problems + " * The row with key '" + r.key() + "'' in the 'pt-index' table contains '"
						+ r.get(col1) + "', but we expected '" + expected.get(r.key()) + "', modulo permutations.\n";
				expected.remove(r.key());
			} else
				expected.remove(r.key());
		}

		for (String s : expected.keySet())
			problems = problems + " * '" + s
					+ "' is a word in our input data, but we couldn't find a row with that key in the 'pt-index' table after running the job.\n";

		return problems;
	}

	String compareRanksToExpected(HashMap<String, Double> expected, double maxDiff)
			throws FileNotFoundException, IOException {
		KVSClient kvs = new KVSClient("localhost:8000");
		Iterator<Row> iter = kvs.scan("pt-pageranks", null, null);
		String problems = "";
		while (iter.hasNext()) {
			Row r = iter.next();
			if (!expected.containsKey(r.key()))
				problems = problems + " * The 'pt-pageranks' table contains a row with key '" + r.key()
						+ "', but this wasn't a URL our input data\n";
			else if (r.get("rank") == null)
				problems = problems + " * In the 'pt-pageranks' table, the row with key '" + r.key()
						+ "' doesn't have a 'rank' column\n";
			else if ((Math.abs(expected.get(r.key()) - Double.valueOf(r.get("rank")))) > maxDiff) {
				problems = problems + " * The rank for page '" + r.key() + "'' is " + r.get("rank")
						+ " in the 'pt-pageranks' table, but we expected '" + expected.get(r.key()) + ", +/-" + maxDiff
						+ "\n";
				expected.remove(r.key());
			} else
				expected.remove(r.key());
		}

		for (String s : expected.keySet())
			problems = problems + " * '" + s
					+ "' is a URL in our input data, but we couldn't find a row with that key in the 'pt-pageranks' table after running the job.\n";

		return problems;
	}

	void runTests(Set<String> tests) throws Exception {

		System.out.printf("\n%-10s%-40sResult\n", "Test", "Description");
		System.out.println("--------------------------------------------------------");

		if (tests.contains("indexer")) try{
			startTest("indexer", "Indexer: Basic tests", 25);
			(new KVSClient("localhost:8000")).delete("pt-index");

			HashMap<String, String> data = new HashMap<String, String>();
			data.put("http://foo.com/page1.html", "apples bananas coconuts");
			data.put("http://foo.com/page2.html", "dates elderberries figs");
			data.put("http://foo.com/page3.html", "guavas hazelnuts");
			installCrawl(data);

			try {
				String output = FlameSubmit.submit("localhost:9000", "indexer.jar", "cis5550.jobs.Indexer",
						new String[] {});
				if (output == null)
					testFailed("Looks like we weren't able to submit 'indexer.jar'; the response code was "
							+ FlameSubmit.getResponseCode() + ", and the output was:\n\n"
							+ FlameSubmit.getErrorResponse());
			} catch (FileNotFoundException fnfe) {
				testFailed("Looks like 'indexer.jar' was not found in the current directory.");
			}

			HashMap<String, String> expected = new HashMap<String, String>();
			expected.put("apples", "http://foo.com/page1.html");
			expected.put("bananas", "http://foo.com/page1.html");
			expected.put("coconuts", "http://foo.com/page1.html");
			expected.put("dates", "http://foo.com/page2.html");
			expected.put("elderberries", "http://foo.com/page2.html");
			expected.put("figs", "http://foo.com/page2.html");
			expected.put("guavas", "http://foo.com/page3.html");
			expected.put("hazelnuts", "http://foo.com/page3.html");

			String problems = compareToExpected(expected, true, true);

			if (!problems.equals(""))
				testFailed(
						"We ran the indexer with some simple test data (all words in lower case, no HTML tags, no punctuation), "
								+ "but the results don't match what we expected. The specific problems we found were:\n\n"
								+ problems);

			testSucceeded();
		}catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }


		if (tests.contains("pagerank")) try{
			startTest("pagerank", "PageRank: Basic tests", 40);
			(new KVSClient("localhost:8000")).delete("pt-pageranks");

			HashMap<String, String> data = new HashMap<String, String>();
			data.put("http://foo.com:80/page1.html",
					"This links to <a href=\"http://foo.com:80/page2.html\">page 2</a>");
			data.put("http://foo.com:80/page2.html",
					"A link to <a href=\"http://foo.com:80/page1.html\">page 1</a> and <a href=\"http://foo.com:80/page3.html\">page 3</a>");
			data.put("http://foo.com:80/page3.html",
					"Linking back to <a href=\"http://foo.com:80/page1.html\">page 1</a>");
			installCrawl(data);

			try {
				System.out.println("submitted");
				String output = FlameSubmit.submit("localhost:9000", "pagerank.jar", "cis5550.jobs.PageRank",
						new String[] { "0.001" });
				System.out.println("returned");
				if (output == null)
					testFailed("Looks like we weren't able to submit 'pagerank.jar'; the response code was "
							+ FlameSubmit.getResponseCode() + ", and the output was:\n\n"
							+ FlameSubmit.getErrorResponse());
				System.out.println("passed? "+output);
			} catch (FileNotFoundException fnfe) {
				testFailed("Looks like 'pagerank.jar' was not found in the current directory.");
			}

			HashMap<String, Double> expected = new HashMap<String, Double>();
			expected.put(Hasher.hash("http://foo.com:80/page1.html"), 1.191681575822917);
			expected.put(Hasher.hash("http://foo.com:80/page2.html"), 1.1637322274926893);
			expected.put(Hasher.hash("http://foo.com:80/page3.html"), 0.644586196684393);

			String problems = compareRanksToExpected(expected, 0.001);

			if (!problems.equals(""))
				testFailed(
						"We ran PageRank with the three-node test graph from the slides, but the results don't match what we expected. "
								+ "The specific problems we found were:\n\n" + problems);

			testSucceeded();
		}catch (Exception e) { testFailed("An exception occurred: "+e, false); e.printStackTrace(); }


		System.out.println("--------------------------------------------------------\n");
	    if (numTestsFailed == 0)
	      System.out.println("Looks like your solution passed all of the selected tests. Congratulations!");
	    else
	      System.out.println(numTestsFailed+" test(s) failed.");

	    cleanup();
	    closeOutputFile();
	}

	public static void main(String args[]) throws Exception {

		/*
		 * Make a set of enabled tests. If no command-line arguments were specified, run
		 * all tests.
		 */

		/*
		 * Make a set of enabled tests. If no command-line arguments were specified, run
		 * all tests.
		 */

		Set<String> tests = new TreeSet<String>();
		boolean runSetup = true, runTests = true, promptUser = true, outputToFile = true, exitUponFailure = true,
				cleanup = true;

		if ((args.length > 0) && args[0].equals("auto")) {
			runSetup = false;
			runTests = true;
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
			System.out.println("HW9 autograder v1.1a (Nov 3, 2023)");
			System.exit(1);
		}

		if ((args.length == 0) || args[0].equals("auto") || args[0].equals("all")) {
			tests.add("indexer");
			tests.add("casereg");
			tests.add("dupwords");
			tests.add("tagfilter");
			tests.add("punct");
			tests.add("white");
			tests.add("complex");
			tests.add("pagerank");
			tests.add("linknorm");
			tests.add("thresh");
			tests.add("srcsink");
			tests.add("larger");
			tests.add("ec1");
			tests.add("ec2");
			tests.add("ec3");
		}
		// ENDIF

		for (int i = 0; i < args.length; i++)
			if (!args[i].equals("all") && !args[i].equals("auto") && !args[i].equals("setup")
					&& !args[i].equals("cleanup"))
				tests.add(args[i]);

		HW9Test t = new HW9Test();
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
