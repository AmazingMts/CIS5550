package cis5550.flame;

import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.HTTP;
import cis5550.tools.Partitioner;
import cis5550.tools.Serializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cis5550.flame.Coordinator.kvs;


public class FlameRDDImpl implements FlameRDD {
    FlameContext context;
    String tableName;
    public FlameRDDImpl(String tableName,FlameContext context) {
        this.tableName = tableName;
        this.context = context;
    }
    @Override
    public List<String> collect() throws Exception {
        KVSClient kvs = new KVSClient("localhost:8000");
        Iterator<Row> iterator = kvs.scan(tableName);
        List<String> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            String key = row.key();  // 获取行键
            String value = row.get("value");  // 获取 "value" 列的内容
//            System.out.println("Key: " + key + ", Value: " + value);  // 打印行键和对应的值
            result.add(value);
        }
        return result;
    }

    @Override
    public FlameRDD flatMap(StringToIterable lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        return (FlameRDD) ((FlameContextImpl) context).invokeOperation("/rdd/flatMap", serializedLambda, this.tableName);

    }

//    List<String> out = rdd.collect();
//		Collections.sort(out);
//
//    String result = "";
//		for (String s : out)
//    result = result+(result.equals("") ? "" : ",")+s;
//
//		ctx.output(result);






    @Override
    public FlamePairRDD mapToPair(StringToPair lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl rdd = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/mapToPair", serializedLambda, this.tableName);
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }

    @Override
    public FlameRDD intersection(FlameRDD r) throws Exception {

        String otherTableName = ((FlameRDDImpl) r).tableName;
        FlameRDDImpl rdd=(FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/intersection", new byte[0], this.tableName,otherTableName);



        String outputTable = rdd.tableName;
        String tempTable = "temp_" + System.currentTimeMillis();
        KVSClient kvs = new KVSClient("localhost:8000");

        // 使用 Set 来跟踪唯一的 value
        Set<String> uniqueValues = new HashSet<>();

        // 遍历 outputTable，将不重复的 value 暂存到 tempTable
        Iterator<Row> iterator = kvs.scan(outputTable);
        while (iterator.hasNext()) {
            Row row = iterator.next();
            String value = row.get("value");

            // 只将唯一的 value 存入 tempTable
            if (uniqueValues.add(value)) {
                String key = UUID.randomUUID().toString();
                kvs.put(tempTable, key, "value", value.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 删除原 outputTable 表
        kvs.delete(outputTable);

        // 重新创建同名的 outputTable 并将 tempTable 的数据插入回去;
        iterator = kvs.scan(tempTable);
        while (iterator.hasNext()) {
            Row row = iterator.next();
            String key = UUID.randomUUID().toString();  // 为每个 entry 生成一个新 key
            String value = row.get("value");
            kvs.put(outputTable, key, "value", value.getBytes(StandardCharsets.UTF_8));
        }

        // 删除临时表 tempTable
        kvs.delete(tempTable);


        // Invoke the operation on the context to perform the intersection on the workers
        return new FlameRDDImpl(rdd.tableName, rdd.context);
    }


    @Override
    public FlameRDD sample(double f) throws Exception {
            return (FlameRDD) ((FlameContextImpl) context).invokeOperation("/rdd/sample", new byte[0], this.tableName, String.valueOf(f));
    }

    @Override
    public FlamePairRDD groupBy(StringToString lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl rdd = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/groupBy", serializedLambda, this.tableName);
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }
}
