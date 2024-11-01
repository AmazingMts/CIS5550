package cis5550.flame;

import java.util.List;
import java.util.Iterator;
import java.io.Serializable;

public interface FlamePairRDD {
  public interface TwoStringsToString extends Serializable {
  	public String op(String a, String b);
  };


  // collect() should return a list that contains all the elements in the PairRDD.

  public List<FlamePair> collect() throws Exception;

  // foldByKey() folds all the values that are associated with a given key in the
  // current PairRDD, and returns a new PairRDD with the resulting keys and values.
  // Formally, the new PairRDD should contain a pair (k,v) for each distinct key k
  // in the current PairRDD, where v is computed as follows: Let v_1,...,v_N be the
  // values associated with k in the current PairRDD (in other words, the current
  // PairRDD contains (k,v_1),(k,v_2),...,(k,v_N)). Then the provided lambda should
  // be invoked once for each v_i, with that v_i as the second argument. The first
  // invocation should use 'zeroElement' as its first argument, and each subsequent
  // invocation should use the result of the previous one. v is the result of the
  // last invocation.

	public FlamePairRDD foldByKey(String zeroElement, TwoStringsToString lambda) throws Exception;
}
