package cis5550.test;

import cis5550.flame.*;
import java.util.*;

public class FlameMapPartitionsTest {
    public static void run(FlameContext ctx, String args[]) throws Exception {
        // 创建数据列表并并行化为 RDD
        List<String> data = Arrays.asList(args);  // 从输入参数构造数据列表
        FlameRDD rdd = ctx.parallelize(data);

        // 使用 mapPartitions 将每个分区的字符串转换为大写
        FlameRDD resultRDD = rdd.mapPartitions(partition -> {
            List<String> results = new ArrayList<>();
            // 遍历每个分区的数据
            while (partition.hasNext()) {
                String value = partition.next();
                results.add(value.toUpperCase());  // 转换为大写
            }
            return results.iterator();  // 返回大写后的数据列表
        });

        // 收集结果并排序
        List<String> output = resultRDD.collect();
        Collections.sort(output);

        // 输出结果
        String result = String.join(",", output);
        ctx.output(result);
    }
}
