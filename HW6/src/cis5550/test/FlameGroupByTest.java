package cis5550.test;

import cis5550.flame.*;
import java.util.List;

public class FlameGroupByTest {
    public static void run(FlameContext ctx, String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Syntax: FlameGroupByTest <listOfInputText>");
            return;
        }

        // 将输入文本按逗号分隔并创建一个 RDD
        String inputText = args[0]; // 输入的文本，用逗号分隔的字符串
        List<String> inputList = List.of(inputText.split(","));
        FlameRDD inputRDD = ctx.parallelize(inputList);

        // 使用 groupBy 按字符串的首字母分组
        FlamePairRDD groupedRDD = inputRDD.groupBy(s -> s.substring(0, 1));  // 以第一个字母分组

        // 收集并输出结果
        List<FlamePair> output = groupedRDD.collect();
        System.out.println("Grouped RDD:");
        for (FlamePair pair : output) {
            System.out.println(pair._1() + " -> " + pair._2());
        }

        // 输出分组后的结果给 FlameContext
        for (FlamePair pair : output) {
            ctx.output(pair._1() + " -> " + pair._2() + "\n");
        }
    }
}
