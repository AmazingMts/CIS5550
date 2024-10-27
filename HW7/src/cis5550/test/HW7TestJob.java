package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class HW7TestJob {
	public static void run(FlameContext ctx, String args[]) throws Exception {
    if (args[0].equals("count")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i++)
				list.add(args[i]);
			int count = ctx.parallelize(list).count();
			ctx.output(""+count);
    } else if (args[0].equals("save")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=2; i<args.length; i++)
				list.add(args[i]);
			ctx.parallelize(list).saveAsTable(args[1]);
			ctx.output("OK");
    } else if (args[0].equals("take")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=2; i<args.length; i++)
				list.add(args[i]);
			List<String> out = ctx.parallelize(list).take(Integer.valueOf(args[1]));
			String ret = "";
			for (String s : out) 
				ret = ret + (ret.equals("") ? "" : ",") + s;
			ctx.output(ret);
    } else if (args[0].equals("fromtable")) {
			List<String> out = ctx.fromTable(args[1], r -> r.columns().iterator().next()).collect();
			Collections.sort(out);
			String ret = "";
			for (String s : out) 
				ret = ret + (ret.equals("") ? "" : ",") + s;
			ctx.output(ret);
    } else if (args[0].equals("map1")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i++)
				list.add(args[i]);
			List<FlamePair> out = ctx.parallelize(list).flatMapToPair(s -> Arrays.asList(new FlamePair[] { new FlamePair(""+s.charAt(0), s.substring(1)) })).collect();
			Collections.sort(out);
			String ret = "";
			for (FlamePair p : out) 
				ret = ret + (ret.equals("") ? "" : ",") + "(" + p._1() + "," + p._2() + ")";
			ctx.output(ret);
    } else if (args[0].equals("map2")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i+=2)
				list.add(args[i]+","+args[i+1]);
			List<String> out = ctx.parallelize(list).mapToPair(s -> new FlamePair(s.split(",")[0], s.split(",")[1])).flatMap(p -> Arrays.asList(new String[] { p._1()+p._2(), p._2()+p._1() })).collect();
			Collections.sort(out);
			String ret = "";
			for (String s : out) 
				ret = ret + (ret.equals("") ? "" : ",") + s;
			ctx.output(ret);
    } else if (args[0].equals("map3")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i+=2)
				list.add(args[i]+","+args[i+1]);
			List<FlamePair> out = ctx.parallelize(list).mapToPair(s -> new FlamePair(s.split(",")[0], s.split(",")[1])).flatMapToPair(p -> Arrays.asList(new FlamePair[] { new FlamePair(p._1(), ""+p._1().length()), new FlamePair(p._2(), ""+p._2().length()) })).collect();
			Collections.sort(out);
			String ret = "";
			for (FlamePair p : out) 
				ret = ret + (ret.equals("") ? "" : ",") + "(" + p._1() + "," + p._2() + ")";
			ctx.output(ret);
    } else if (args[0].equals("distinct")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i++)
				list.add(args[i]);
			List<String> out = ctx.parallelize(list).distinct().collect();
			Collections.sort(out);
			String ret = "";
			for (String s : out) 
				ret = ret + (ret.equals("") ? "" : ",") + s;
			ctx.output(ret);
    } else if (args[0].equals("join")) {
			LinkedList<String> list1 = new LinkedList<String>();
			LinkedList<String> list2 = new LinkedList<String>();
			for (int i=0; i<Integer.valueOf(args[1]); i+=2)
				list1.add(args[3+i]+","+args[4+i]);
			for (int i=0; i<Integer.valueOf(args[2]); i+=2)
				list2.add(args[3+i+Integer.valueOf(args[1])]+","+args[4+i+Integer.valueOf(args[1])]);
			FlamePairRDD rdd1 = ctx.parallelize(list1).mapToPair(s -> new FlamePair(s.split(",")[0], s.split(",")[1]));
			FlamePairRDD rdd2 = ctx.parallelize(list2).mapToPair(s -> new FlamePair(s.split(",")[0], s.split(",")[1]));
			List<FlamePair> out = rdd1.join(rdd2).collect();
			Collections.sort(out);
			String ret = "";
			for (FlamePair p : out) 
				ret = ret + (ret.equals("") ? "" : ",") + "(" + p._1() + ",\"" + p._2() + "\")";
			ctx.output(ret);
    } else if (args[0].equals("fold")) {
			LinkedList<String> list = new LinkedList<String>();
			for (int i=1; i<args.length; i++)
				list.add(args[i]);
			String out = ctx.parallelize(list).fold("0", (s1,s2) -> ""+(Integer.valueOf(s1) + Integer.valueOf(s2)));
			ctx.output(out);
    } else {
    	ctx.output("Unknown test case '"+args[0]+" - HW7TestJob only supports 'count' and 'save'");
    }
	}
}
