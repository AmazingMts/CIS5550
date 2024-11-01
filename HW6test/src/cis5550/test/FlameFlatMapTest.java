package cis5550.test;

import cis5550.flame.FlameContext;
import cis5550.flame.FlameContextImpl;
import cis5550.flame.FlameRDD;

import java.util.Arrays;
import java.util.List;

public class FlameFlatMapTest {
    public static void main(String[] args) throws Exception {
        // 通过上下文创建 RDD
        FlameContext context = new FlameContextImpl("testJob");

        // 使用 parallelize 创建 RDD
        FlameRDD rdd = context.parallelize(Arrays.asList("hello world", "foo bar"));

        // 执行 flatMap 操作，将每个字符串拆分为单词
        FlameRDD flatMappedRDD = rdd.flatMap(s -> Arrays.asList(s.split(" ")));

        // 收集 flatMap 的结果
        List<String> result = flatMappedRDD.collect();

        // 输出结果
        System.out.println("FlatMap 结果: " + result);

        // 简单的断言测试，验证是否得到了期望的结果
        if (result.contains("hello") && result.contains("world") && result.contains("foo") && result.contains("bar")) {
            System.out.println("测试通过！");
        } else {
            System.out.println("测试失败！");
        }
    }
}
