//package cis5550.test;
//
//import cis5550.flame.*;
//import java.util.*;
//
//public class FlameCogroupTest {
//    public static void run(FlameContext ctx, String[] args) throws Exception {
//        // Input data for the test as strings, to be converted into pairs
//        List<String> data1 = Arrays.asList("fruit:apple", "fruit:banana", "vegetable:carrot");
//        List<String> data2 = Arrays.asList("fruit:cherry", "fruit:date", "vegetable:broccoli");
//
//        // Parallelize and convert to FlamePairRDD using mapToPair
//        FlamePairRDD rdd1 =  ctx.parallelize(data1).mapToPair(s -> {
//            String[] split = s.split(":");
//            return new FlamePair(split[0], split[1]);
//        });
//
//        FlamePairRDD rdd2 = ctx.parallelize(data2).mapToPair(s -> {
//            String[] split = s.split(":");
//            return new FlamePair(split[0], split[1]);
//        });
//
//        // Perform cogroup operation
//        FlamePairRDD resultRDD = ((FlamePairRDD) rdd1).cogroup((FlamePairRDD) rdd2);
//
//        // Collect and output results
//        List<FlamePair> output = resultRDD.collect();
//        Collections.sort(output);
//
//        // Format and output result as a string
//        StringBuilder result = new StringBuilder();
//        for (FlamePair p : output) {
//            result.append(p.toString()).append("\n");
//        }
//        ctx.output(result.toString());
//    }
//}
