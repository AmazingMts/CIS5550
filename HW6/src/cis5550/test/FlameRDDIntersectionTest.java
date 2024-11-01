package cis5550.test;

import cis5550.flame.*;
import java.util.Arrays;
import java.util.List;

public class FlameRDDIntersectionTest {
    public static void run(FlameContext ctx, String args[]) throws Exception {
        // Ensure we have enough input arguments to test
        if (args.length < 2) {
            System.err.println("Syntax: FlameRDDIntersectionTest <list1> <list2>");
            return;
        }

        // Create two FlameRDDs from the input arguments
        FlameRDD rdd1 = ctx.parallelize(Arrays.asList(args[0].split(",")));
        FlameRDD rdd2 = ctx.parallelize(Arrays.asList(args[1].split(",")));

        // Perform intersection
        FlameRDD intersectionRDD = rdd1.intersection(rdd2);

        // Collect the results of the intersection
        List<String> output = intersectionRDD.collect();

        // Output the intersection results
        for (String value : output) {
            ctx.output(value + "\n");
        }
    }
}
