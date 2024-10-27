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

  public interface StringToBoolean extends Serializable {
    boolean op(String a) throws Exception;
  }

  public interface IteratorToIterator extends Serializable {
    Iterator<String> op(Iterator<String> a) throws Exception;
  }

  // count() should return the number of elements in this RDD. 
  // Duplicate elements should be included in the count.

  public int count() throws Exception;

  // saveAsTable() should cause a table with the specified name to appear 
  // in the KVS that contains the data from this RDD. The table should 
  // have a row for each element of the RDD, and the element should be
  // in a column called 'value'; the key can be anything. 

  public void saveAsTable(String tableNameArg) throws Exception;

  // distinct() should return a new RDD that contains the same
  // elements, except that, if the current RDD contains multiple
  // copies of some elements, the new RDD should contain only 
  // one copy of those elements.

  public FlameRDD distinct() throws Exception;

  // destroy() should delete the underlying table in the key-value store.
  // Any future invocations of any method on this RDD should throw an
  // exception.

  public void destroy() throws Exception;

  // take() should return up 'num' elements from the RDD, or all
  // elements, if the RDD contains fewer than 'num' elements.
  // If the RDD contains more than 'num' elements, any subset
  // of size 'num' may be returned.

  public Vector<String> take(int num) throws Exception;

  // fold() should call the provided lambda for each element of the 
  // RDD, with that element as the second argument. In the first
  // invocation, the first argument should be 'zeroElement'; in
  // each subsequent invocation, the first argument should be the
  // result of the previous invocation. The function returns
  // the result of the last invocation, or 'zeroElement' if the 
  // RDD does not contain any elements.

  public String fold(String zeroElement, FlamePairRDD.TwoStringsToString lambda) throws Exception;

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

  // flatMapToPair() is analogous to flatMap(), except that the lambda
  // returns pairs instead of strings, and tha tthe output is a PairRDD
  // instead of a normal RDD.

  public FlamePairRDD flatMapToPair(StringToPairIterable lambda) throws Exception;

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

  // filter() should accept a boolean predicate and should return 
  // another RDD that contains only those elements from the original RDD 
  // on which the predicate evaluates to true. This method is extra credit
  // on HW7 and should return 'null' if you did not do this EC.

  public FlameRDD filter(StringToBoolean lambda) throws Exception;

  // mapPartitions() should take a lambda that is given an Iterator<String>
  // and returns another Iterator<String>. The lambda should be invoked once 
  // on each worker, with an iterator that contains the RDD elements 
  // that worker is working on (see KVSClient.scan()); the elements 
  // in the iterator that the lamblda returns should be stored in 
  // another RDD, which mapPartitions() should return. This method is 
  // extra credit on HW7 and should return 'null' if you did not do this EC.

  public FlameRDD mapPartitions(IteratorToIterator lambda) throws Exception;
}
