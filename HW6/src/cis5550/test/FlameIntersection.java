package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameIntersection {
    public static void run(FlameContext ctx, String args[]) throws Exception {
        // 初始化两个数据列表
        LinkedList<String> list1 = new LinkedList<>();
        LinkedList<String> list2 = new LinkedList<>();

        // 前半部分数据给 list1，后半部分数据给 list2
        int midIndex = args.length / 2;
        for (int i = 0; i < midIndex; i++) {
            list1.add(args[i]);  // 前半部分放入 list1
        }
        for (int i = midIndex; i < args.length; i++) {
            list2.add(args[i]);  // 后半部分放入 list2
        }

        // 使用 ctx.parallelize 将数据列表并行化为 RDD
        FlameRDD rdd1 = ctx.parallelize(list1);
        FlameRDD rdd2 = ctx.parallelize(list2);

        // 执行 intersection 操作获取交集
        FlameRDD resultRDD = rdd1.intersection(rdd2);

        // 收集结果并排序
        List<String> result = resultRDD.collect();
        Collections.sort(result);

        // 格式化输出结果
        String output = String.join(",", result);
        ctx.output(output);
    }
}
