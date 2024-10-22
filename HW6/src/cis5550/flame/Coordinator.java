package cis5550.flame;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

import static cis5550.webserver.Server.*;
import cis5550.kvs.KVSClient;
import cis5550.tools.*;

class Coordinator extends cis5550.generic.Coordinator {


  private static final Logger logger = Logger.getLogger(Coordinator.class);
  private static final String version = "v1.5 Jan 1 2023";

  static int nextJobID = 1;
  public static KVSClient kvs;

	public static void main(String args[]) {
      // Check the command-line arguments

    if (args.length != 2) {
      System.err.println("Syntax: Coordinator <port> <kvsCoordinator>");
      System.exit(1);
    }
   
    int myPort = Integer.valueOf(args[0]);
    kvs = new KVSClient(args[1]);

    logger.info("Flame coordinator ("+version+") starting on port "+myPort);

  	port(myPort);
    registerRoutes();



    get("/", (request,response) -> {
      response.type("text/html");
      return "<html><head><title>Flame coordinator</title></head><body><h3>Flame Coordinator</h3>\n" + clientTable() + "</body></html>";
    });

    /* Set up the main route for job submissions. This is invoked from FlameSubmit. */

    post("/submit", (request,response) -> {

      // Extract the parameters from the query string. The 'class' parameter, which contains the main class, is mandatory, and
      // we'll send a 400 Bad Request error if it isn't present. The 'arg1', 'arg2', ..., arguments contain command-line
      // arguments for the job and are optional.

      String className = request.queryParams("class");
      logger.info("New job submitted; main class is "+className);

      if (className == null) {
        response.status(400, "Bad request");
        return "Missing class name (parameter 'class')";
      }

      Vector<String> argVector = new Vector<String>();
      for (int i=1; request.queryParams("arg"+i) != null; i++) 
        argVector.add(URLDecoder.decode(request.queryParams("arg"+i), "UTF-8"));

      // We begin by uploading the JAR to each of the workers. This should be done in parallel, so we'll use a separate
      // thread for each upload.

      Thread threads[] = new Thread[getWorkers().size()];
      String results[] = new String[getWorkers().size()];
      for (int i=0; i<getWorkers().size(); i++) {
        final String url = "http://"+getWorkers().elementAt(i)+"/useJAR";
        final int j = i;
        threads[i] = new Thread("JAR upload #"+(i+1)) {
          public void run() {
            try {
              results[j] = new String(HTTP.doRequest("POST", url, request.bodyAsBytes()).body());
            } catch (Exception e) {
              results[j] = "Exception: "+e;
              e.printStackTrace();
            }
          }
        };
        threads[i].start();
      }

      // Wait for all the uploads to finish

      for (int i=0; i<threads.length; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException ie) {
        }
      }

      // Write the JAR file to a local file. Remember, we will need to invoke the 'run' method of the job, but, if the job
      // is submitted from a machine other than the coordinator, the coordinator won't have a local copy of the JAR file. We'll use
      // a different file name each time.

      int id = nextJobID++;
      String jarName = "job-"+id+".jar";
      File jarFile = new File(jarName);
      FileOutputStream fos = new FileOutputStream(jarFile);
      fos.write(request.bodyAsBytes());
      fos.close();

      // Load the class whose name the user has specified with the 'class' parameter, find its 'run' method, and 
      // invoke it. The parameters are 1) an instance of a FlameContext, and 2) the command-line arguments that
      // were provided in the query string above, if any. Several things can go wrong here: the class might not 
      // exist or might not have a run() method, and the method might throw an exception while it is running,
      // in which case we'll get an InvocationTargetException. We'll extract the underlying cause and report it
      // back to the user in the HTTP response, to help with debugging.
      FlameContextImpl context = new FlameContextImpl(jarName);
      try {
        Loader.invokeRunMethod(jarFile, className, context, argVector);
      } catch (IllegalAccessException iae) {
        response.status(400, "Bad request");
        return "Double-check that the class "+className+" contains a public static run(FlameContext, String[]) method, and that the class itself is public!";
      } catch (NoSuchMethodException iae) {
        response.status(400, "Bad request");
        return "Double-check that the class "+className+" contains a public static run(FlameContext, String[]) method";
      } catch (InvocationTargetException ite) {
        logger.error("The job threw an exception, which was:", ite.getCause());
        StringWriter sw = new StringWriter();
        ite.getCause().printStackTrace(new PrintWriter(sw));
        response.status(500, "Job threw an exception");
        return sw.toString();
      }
      List<String> outputs = context.getOutputs();
      StringBuilder result = new StringBuilder();
        for (String output : outputs) {
            result.append(output);
        }
      return result.toString();

    });

    get("/version", (request, response) -> { return "v1.2 Oct 28 2022"; });


	}

}
