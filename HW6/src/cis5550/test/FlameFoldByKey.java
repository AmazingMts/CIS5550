package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameFoldByKey {
	public static void run(FlameContext ctx, String args[]) throws Exception {
		LinkedList<String> list = new LinkedList<String>();
		for (int i=0; i<args.length; i++)
			list.add(args[i]);
	
		List<FlamePair> out = ctx.parallelize(list)
    		                 .mapToPair(s -> { String[] pieces = s.split(" "); return new FlamePair(pieces[0], pieces[1]);	})
		                     .foldByKey("0", (a,b) -> ""+(Integer.valueOf(a)+Integer.valueOf(b)))
		                     .collect();
	
		Collections.sort(out);
	
		String result = "";
		for (FlamePair p : out) 
			result = result+(result.equals("") ? "" : ",")+p.toString();

		ctx.output(result);
	}
}