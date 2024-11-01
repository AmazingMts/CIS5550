package cis5550.tools;

import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

public class Logger {

  protected static HashMap<String,Logger> prefixToLogger = null;
  protected static PrintWriter logfile = null;
  protected static Logger defaultLogger = null;
  protected static SimpleDateFormat dateFormat = null;

  protected static final int ALL = 6;
  protected static final int DEBUG = 5;
  protected static final int INFO = 4;
  protected static final int WARN = 3;
  protected static final int ERROR = 2;
  protected static final int FATAL = 1;
  protected static final int OFF = 0;
 
  int upToLevel;

  protected Logger(int upToLevelArg) {
  	upToLevel = upToLevelArg;
  }

  protected void write(int level, String line, Throwable t) {
    if ((upToLevel == OFF) || (level > upToLevel))
    	return;

    String stackTrace = null;
    if (t != null) {
    	StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      stackTrace = sw.toString();
    }

    String logFilePrefix = dateFormat.format(new Date())+" "+Thread.currentThread().getName();
    while (logFilePrefix.length() < 30)
      logFilePrefix = logFilePrefix + " ";
    if (level == WARN)
    	logFilePrefix = logFilePrefix + "WARNING: ";
    else if (level == ERROR)
    	logFilePrefix = logFilePrefix + "ERROR: ";
    else if (level == FATAL)
    	logFilePrefix = logFilePrefix + "FATAL: ";
    else if (level == DEBUG)
    	logFilePrefix = logFilePrefix + "  ";

    synchronized(defaultLogger) {
      if (logfile != null) {
   	    logfile.println(logFilePrefix + line);
   	    if (stackTrace != null) {
   	    	logfile.print(stackTrace);
   	    	logfile.flush();
   	    }
      }
      if ((logfile == null) || (level <= ERROR)) {
        System.err.println(logFilePrefix + line);
        if (stackTrace != null)
        	System.err.print(stackTrace);
      }
    }
  }

  public void fatal(String message, Throwable t) {
    write(FATAL, message, t);
  }

  public void fatal(String message) {
  	write(FATAL, message, null);
  }

  public void error(String message, Throwable t) {
    write(ERROR, message, t);
  }

  public void error(String message) {
  	write(ERROR, message, null);
  }

  public void warn(String message, Throwable t) {
    write(WARN, message, t);
  }

  public void warn(String message) {
  	write(WARN, message, null);
  }

  public void info(String message, Throwable t) {
  	write(INFO, message, t);
  }

  public void info(String message) {
  	write(INFO, message, null);
  }

  public void debug(String message, Throwable t) {
  	write(DEBUG, message, t);
  }

  public void debug(String message) {
  	write(DEBUG, message, null);
  }

  protected static String getMainClassName()
  {
    StackTraceElement trace[] = Thread.currentThread().getStackTrace();
    if (trace.length > 0) {
      return trace[trace.length - 1].getClassName();
    }
    return "Unknown";
  } 

  public static Logger getLogger(Class c) {
    if (prefixToLogger == null) {
    	defaultLogger = new Logger(ERROR);
      logfile = null;
    	prefixToLogger = new HashMap<String,Logger>();
    	dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
    	try {
    	  BufferedReader r = new BufferedReader(new FileReader("log.properties"));
    	  while (true) {
    	  	String line = null;
    	  	try { line = r.readLine(); } catch (IOException ioe) {}
    	  	if (line == null)
    	  		break;
          line = line.trim();
          if (line.startsWith("#") || line.equals(""))
            continue;

    	  	String[] pcs = line.split("=");
    	  	pcs[0] = pcs[0].trim();
    	  	pcs[1] = pcs[1].trim();
          if (pcs[0].equals("log")) {
            String logfileName = pcs[1].replaceAll("\\$MAINCLASS", getMainClassName()).replaceAll("\\$PID", ""+ProcessHandle.current().pid());
          	try {
              logfile = new PrintWriter(new FileWriter(logfileName, true), true);
            } catch (Exception e) {
            	System.err.println("Cannot create log file: '"+logfileName+"'");
            	System.exit(1);
            }
          } else {
            String[] levels = new String[] { "off", "fatal", "error", "warn", "info", "debug", "all" };
            boolean found = false;
            for (int i=0; i<levels.length; i++) {
            	if (pcs[1].equalsIgnoreCase(levels[i])) {
          	    prefixToLogger.put(pcs[0], new Logger(i));
          	    found = true;
            	}
            }
            if (!found) {
            	System.err.println("Invalid loglevel '"+pcs[1]+"' for prefix '"+pcs[0]+"' in '"+pcs[0]+"'");
            	System.exit(1);
            }
          }
    	  }
    	  try { r.close(); } catch (IOException ioe) {}
    	} catch (FileNotFoundException fnfe) {
    		// OK not to have logging enabled
    	}
    }

    String[] els = c.getName().split("\\.");
    String prefix = "";
    for (int i=0; i<els.length; i++) {
    	prefix = prefix + ((i==0) ? "" : ".") + els[i];
    	if (prefixToLogger.get(prefix) != null)
    		return prefixToLogger.get(prefix);
    }

    /* No specific entry found; just do errors and above */

    return defaultLogger;
  }
}