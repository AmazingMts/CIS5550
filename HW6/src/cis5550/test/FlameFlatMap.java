package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameFlatMap {
	public static void run(FlameContext ctx, String args[]) throws Exception {
		LinkedList<String> list = new LinkedList<String>();
		for (int i=0; i<args.length; i++)
			list.add(args[i]);
	
		FlameRDD rdd = ctx.parallelize(list).flatMap(s -> Arrays.asList(s.split(" ")));
	
		List<String> out = rdd.collect();
		Collections.sort(out);
	
		String result = "";
		for (String s : out) 
			result = result+(result.equals("") ? "" : ",")+s;

		ctx.output(result);
	}
}