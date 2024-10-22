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
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }
}
