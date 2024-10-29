package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameCogroupTest {
    public static void run(FlameContext ctx, String[] args) throws Exception {
        // Prepare sample data for the first RDD
        List<FlamePair> data1 = Arrays.asList(
                new FlamePair("fruit", "apple"),
                new FlamePair("fruit", "banana"),
                new FlamePair("color", "red")
        );

        // Prepare sample data for the second RDD
        List<FlamePair> data2 = Arrays.asList(
                new FlamePair("fruit", "cherry"),
                new FlamePair("fruit", "date"),
                new FlamePair("fruit", "fig"),
                new FlamePair("animal", "dog")
        );

        // Parallelize data to create two PairRDDs
        FlamePairRDD rdd1 = ctx.parallelizePairs(data1);
        FlamePairRDD rdd2 = ctx.parallelizePairs(data2);

        // Perform cogroup operation
        FlamePairRDD resultRDD = rdd1.cogroup(rdd2);

        // Collect the result and sort for predictable output
        List<FlamePair> output = resultRDD.collect();
        output.sort(Comparator.comparing(FlamePair::_1));

        // Format and output results for verification
        StringBuilder result = new StringBuilder();
        for (FlamePair pair : output) {
            result.append(pair._1()).append(": ").append(pair._2()).append("\n");
        }

        // Print the final result
        ctx.output(result.toString());
    }
}
