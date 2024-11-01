package cis5550.flame;

import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.Serializer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FlamePairRDDImpl implements FlamePairRDD {

    private FlameContext context;
    private String tableName;

    public FlamePairRDDImpl(String tableName, FlameContext context) {
        this.tableName = tableName;
        this.context = context;
    }

//    public static void main(String[] args) {
//        try {
//            // Initialize FlamePairRDDImpl with your table name and context
//            FlameContext context = new FlameContextImpl("job-1.jar");  // Assuming you have a context implementation
//            String tableName = "testTable";  // Replace with your actual table name
//
//            // Create an instance of FlamePairRDDImpl
//            FlamePairRDDImpl pairRDD = new FlamePairRDDImpl(tableName, context);
//
//            List<FlamePair> result = pairRDD.collect();
//
//            // Print the collected key-value pairs
//            System.out.println("Collected FlamePairs:");
//            for (FlamePair pair : result) {
//                System.out.println(pair._1() + " -> " + pair._2());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public List<FlamePair> collect() throws Exception {
        KVSClient kvs = new KVSClient("localhost:8000");
        Iterator<Row> iterator = kvs.scan(tableName);
        List<FlamePair> result = new ArrayList<>();

        while (iterator.hasNext()) {
            Row row = iterator.next();
            Set<String> columnKeys = row.columns();
            for (String key : columnKeys) {
                String value = row.get(key);
                result.add(new FlamePair(key, value));
                System.out.println("Key: " + key + ", Value: " + value);  // 打印每个键值对
            }
        }

        return result;
    }



    @Override
    public FlamePairRDD foldByKey(String zeroElement, TwoStringsToString lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        String encodedZero = URLEncoder.encode(zeroElement, StandardCharsets.UTF_8);
        // 通过 invokeOperation 调用 foldByKey
        FlameRDDImpl rdd = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/foldByKey", serializedLambda, this.tableName,encodedZero);

        String outputTable = rdd.tableName;
        Map<String, Integer> aggregatedResults = new HashMap<>();

        // 获取表的所有行，并根据 columnKey 聚合数值
        KVSClient kvs = new KVSClient("localhost:8000");
        Iterator<Row> iterator = kvs.scan(outputTable);

        while (iterator.hasNext()) {
            Row row = iterator.next();
            String key = row.key();

            // 遍历行的所有列，根据 columnKey 聚合数值
            Set<String> columns = row.columns();
            for (String columnKey : columns) {
                String value = row.get(columnKey);
                try {
                    int intValue = Integer.parseInt(value);

                    aggregatedResults.put(columnKey, aggregatedResults.getOrDefault(columnKey, 0) + intValue);
                } catch (NumberFormatException e) {
                    System.err.println("非数值格式的值: " + value + " 已被忽略。");
                }
            }
        }


        kvs.delete(outputTable);
        for (Map.Entry<String, Integer> entry : aggregatedResults.entrySet()) {
            String columnKey = entry.getKey();
            String aggregatedValue = String.valueOf(entry.getValue()); // 将累加结果转换为字符串
            kvs.put(outputTable,"value" , columnKey, aggregatedValue.getBytes(StandardCharsets.UTF_8));
        }



        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }
}
