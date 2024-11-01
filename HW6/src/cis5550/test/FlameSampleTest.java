package cis5550.test;

import cis5550.flame.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class FlameSampleTest {
    public static void run(FlameContext ctx, String args[]) throws Exception {
        // 检查传入的参数是否符合要求
        if (args.length < 2) {
            System.err.println("Syntax: FlameSampleTest <listOfInputText> <samplingProbability>");
            return;
        }

        // 解析输入参数
        String inputText = args[0]; // 输入的文本，用逗号分隔的字符串
        double samplingProbability = Double.parseDouble(args[1]); // 采样概率

        // 将输入文本按逗号分隔，并创建一个 RDD
        List<String> inputList = Arrays.asList(inputText.split(","));
        FlameRDD inputRDD = ctx.parallelize(inputList);

        // 执行采样操作
        FlameRDD sampledRDD = inputRDD.sample(samplingProbability);

        // 收集并输出采样结果
        List<String> output = sampledRDD.collect();
        System.out.println("Sampled RDD:");
        for (String value : output) {
            System.out.println(value);
        }

        // 输出采样后的结果给 FlameContext
        for (String value : output) {
            ctx.output(value + "\n");
        }
    }
}
