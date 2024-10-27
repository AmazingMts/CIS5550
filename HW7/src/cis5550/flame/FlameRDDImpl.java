package cis5550.flame;

import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class FlameRDDImpl implements FlameRDD {
    FlameContext context;
    String tableName;
    public FlameRDDImpl(String tableName,FlameContext context) {
        this.tableName = tableName;
        this.context = context;
    }

    @Override
    public int count() throws Exception {
        List<String> elements = this.collect();
        // 返回元素总数（包括重复元素）
        return elements.size();
    }

    @Override
    public void saveAsTable(String tableNameArg) throws Exception {
        // 创建 KVS 客户端
        KVSClient kvs = new KVSClient("localhost:8000");

        // 获取 RDD 的所有元素
        List<String> elements = this.collect();

        // 将每个元素插入到指定的表中
        int rowIndex = 0; // 使用递增的行索引作为唯一键
        for (String element : elements) {
            kvs.put(tableNameArg, Integer.toString(rowIndex++), "value", element);
        }
    }

    @Override
    public FlameRDD distinct() throws Exception {
        return (FlameRDD) ((FlameContextImpl) context).invokeOperation("/rdd/distinct", new byte[0], this.tableName);
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public Vector<String> take(int num) throws Exception {
        // 创建 KVS 客户端
        KVSClient kvs = new KVSClient("localhost:8000");

        // 扫描底层表，并获取最多 num 个元素
        Iterator<Row> iterator = kvs.scan(tableName);
        Vector<String> result = new Vector<>();

        // 迭代表中的行，直到达到 num 个元素或没有更多行
        while (iterator.hasNext() && result.size() < num) {
            Row row = iterator.next();
            String value = row.get("value"); // 获取 "value" 列的内容
            result.add(value);
        }

        return result;
    }


    @Override
    public String fold(String zeroElement, FlamePairRDD.TwoStringsToString lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);

        // 使用 invokeOperationOnWorkers 调用所有工人，并收集结果
        List<String> workerResults = (List<String>) ((FlameContextImpl) context).invokeOperation(
                "/pairRDD/fold", serializedLambda, this.tableName, zeroElement
        );

        // 在协调器端聚合所有工人结果
        String finalResult = zeroElement;
        for (String workerResult : workerResults) {
            finalResult = lambda.op(finalResult, workerResult); // 逐次聚合
        }

        return finalResult;
    }


    @Override
    public FlameRDD filter(StringToBoolean lambda) throws Exception {
        return null;
    }

    @Override
    public FlameRDD mapPartitions(IteratorToIterator lambda) throws Exception {
        return null;
    }

    @Override
    public FlamePairRDD flatMapToPair(StringToPairIterable lambda) throws Exception {
        // 序列化 lambda
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);

        FlameRDDImpl resultRDD = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/flatMapToPair", serializedLambda, this.tableName);
        // 返回一个新的 FlamePairRDDImpl 实例，指向新生成的结果表
        return new FlamePairRDDImpl(resultRDD.tableName, resultRDD.context);
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
    public FlameRDD flatMap(StringToIterable lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        return (FlameRDD) ((FlameContextImpl) context).invokeOperation("/rdd/flatMap", serializedLambda, this.tableName);

    }
    @Override
    public FlamePairRDD mapToPair(StringToPair lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl rdd = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/mapToPair", serializedLambda, this.tableName);
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }

    @Override
    public FlameRDD intersection(FlameRDD r) throws Exception {

        String otherTableName = ((FlameRDDImpl) r).tableName;

        // Invoke the operation on the context to perform the intersection on the workers
        return (FlameRDD) ((FlameContextImpl) context).invokeOperation("/rdd/intersection", new byte[0], this.tableName,otherTableName);
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
    @Override
    public List<String> collect() throws Exception {
        KVSClient kvs = new KVSClient("localhost:8000");
        Iterator<Row> iterator = kvs.scan(tableName);
        List<String> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            String key = row.key();  // 获取行键
            String value = row.get("value");  // 获取 "value" 列的内容
            result.add(value);
        }
        return result;
    }


}
