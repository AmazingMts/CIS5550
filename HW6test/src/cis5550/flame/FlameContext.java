package cis5550.flame;

import java.util.*;
import java.io.*;
import cis5550.kvs.KVSClient;

public interface FlameContext {
  public KVSClient getKVS();


  // When a job invokes output(), your solution should store the provided string
  // and return it in the body of the /submit response, if and when the job 
  // terminates normally. If a job invokes output() more than once, the strings
  // should be concatenated. If a job never invokes output(), the body of the
  // /submit response should contain a message saying that there was no output.

  public void output(String s);

  // This function should return a FlameRDD that contains the strings in the provided
  // List. It is okay for this method to run directly on the coordinator; it does not
  // need to be parallelized.

  public FlameRDD parallelize(List<String> list) throws Exception;
}
