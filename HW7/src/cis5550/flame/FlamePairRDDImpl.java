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
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }

    @Override
    public void saveAsTable(String tableNameArg) throws Exception {

    }

    @Override
    public FlameRDD flatMap(PairToStringIterable lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl resultRDD = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation(
                "/pairRDD/flatMap", serializedLambda, this.tableName);
        return new FlameRDDImpl(resultRDD.tableName, resultRDD.context);
    }

    @Override
    public void destroy() throws Exception {
    }

    @Override
    public FlamePairRDD flatMapToPair(PairToPairIterable lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl resultRDD = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation(
                "/pairRDD/flatMapToPair", serializedLambda, this.tableName);
        return new FlamePairRDDImpl(resultRDD.tableName, resultRDD.context);
    }

    @Override
    public FlamePairRDD join(FlamePairRDD other) throws Exception {
        String otherTable= ((FlamePairRDDImpl) other) .tableName;
        FlameRDDImpl resultRDD = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation(
                "/pairRDD/join", new byte[0], this.tableName,otherTable);
        return new FlamePairRDDImpl(resultRDD.tableName, resultRDD.context);
    }

    @Override
    public FlamePairRDD cogroup(FlamePairRDD other) throws Exception {
        return null;
    }
}
