package cis5550.flame;

public class FlamePair implements Comparable<FlamePair> {

  String a, b;

  public String _1() {
  	return a;
  }

  public String _2() {
  	return b;
  }

	public FlamePair(String aArg, String bArg) {
    a = aArg;
    b = bArg;
	}

  public int compareTo(FlamePair o) {
    if (_1().equals(o._1()))
      return _2().compareTo(o._2());
    else
      return _1().compareTo(o._1());
  }

  public String toString() {
    return "("+a+","+b+")";
  }
}