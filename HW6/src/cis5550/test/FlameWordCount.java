package cis5550.test;

import cis5550.flame.*;
import java.util.Arrays;
import java.util.List;

public class FlameWordCount {
	public static void run(FlameContext ctx, String args[]) throws Exception {
		if (args.length < 1) {
			System.err.println("Syntax: FlameWordCount <linesOfInputText>");
			return;
		}

		FlameRDD lines = ctx.parallelize(Arrays.asList(args));
		FlameRDD words = lines.flatMap(s -> {
			String[] pieces = s.split(" ");
			return Arrays.asList(pieces);
		});
		FlamePairRDD ones = words.mapToPair(word -> new FlamePair(word, "1"));
		FlamePairRDD counts = ones.foldByKey("0", (String a, String b) -> {
			int v1 = Integer.parseInt(a);
			int v2 = Integer.parseInt(b);
			return ""+(v1+v2);
		});
		List<FlamePair> output = counts.collect();
		for (FlamePair tuple : output)
			ctx.output(tuple._1()+": "+tuple._2()+"\n");
	}
}