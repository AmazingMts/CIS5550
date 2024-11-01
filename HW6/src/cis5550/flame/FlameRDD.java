package cis5550.flame;

import java.io.Serializable;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;

public interface FlameRDD {
  public interface StringToIterable extends Serializable {
  	Iterable<String> op(String a) throws Exception;
  };

  public interface StringToPair extends Serializable {
  	FlamePair op(String a) throws Exception;
  };

  public interface StringToPairIterable extends Serializable {
    Iterable<FlamePair> op(String a) throws Exception;
  };

  public interface StringToString extends Serializable {
    String op(String a) throws Exception;
  };

  // collect() should return a list that contains all the elements 
  // in the RDD.

  public List<String> collect() throws Exception;

  // flatMap() should invoke the provided lambda once for each element 
  // of the RDD, and it should return a new RDD that contains all the
  // strings from the Iterables the lambda invocations have returned.
  // It is okay for the same string to appear more than once in the output;
  // in this case, the RDD should contain multiple copies of that string.
  // The lambda is allowed to return null or an empty Iterable.

	public FlameRDD flatMap(StringToIterable lambda) throws Exception;

  // mapToPair() should invoke the provided lambda once for each element
  // of the RDD, and should return a PairRDD that contains all the pairs
  // that the lambda returns. The lambda is allowed to return null, and
  // different invocations can return pairs with the same keys and/or the
  // same values.

  public FlamePairRDD mapToPair(StringToPair lambda) throws Exception;

  // ----------------------- EXTRA CREDIT ITEMS ---------------------------

  // intersection() should return an RDD that contains only elements 
  // that are present both 1) in the RDD on which the method is invoked,
  // and 2) in the RDD that is given as an argument. The returned RDD
  // should contain each unique element only once, even if one or both
  // of the input RDDs contain multiple instances. This method is extra
  // credit on HW6 and should return 'null' if this EC is not implemented.

  public FlameRDD intersection(FlameRDD r) throws Exception;

  // sample() should return a new RDD that contains each element in the 
  // original RDD with the probability that is given as an argument.
  // If the original RDD contains multiple instances of the same element,
  // each instance should be sampled individually. This method is extra
  // credit on HW6 and should return 'null' if this EC is not implemented.

  public FlameRDD sample(double f) throws Exception;

  // groupBy() should apply the given lambda to each element in the RDD
  // and return a PairRDD with elements (k, V), where k is a string that
  // the lambda returned for at least one input element and V is a
  // comma-separated list of elements in the original RDD for which the
  // lambda returned k. This method is extra credit on HW6 and should 
  // return 'null' if this EC is not implemented.

  public FlamePairRDD groupBy(StringToString lambda) throws Exception;
}
