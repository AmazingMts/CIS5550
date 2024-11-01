package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameMapToPair {
	public static void run(FlameContext ctx, String args[]) throws Exception {
		LinkedList<String> list = new LinkedList<String>();
		for (int i=0; i<args.length; i++)
			list.add(args[i]);
	
		List<FlamePair> out = ctx.parallelize(list)
                             .mapToPair(s -> new FlamePair(""+s.charAt(0), s.substring(1)))
                             .collect();
	
		Collections.sort(out);
	
		String result = "";
		for (FlamePair p : out) 
			result = result+(result.equals("") ? "" : ",")+p.toString();

		ctx.output(result);
	}
}