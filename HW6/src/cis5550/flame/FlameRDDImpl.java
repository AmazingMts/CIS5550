package cis5550.flame;

import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.HTTP;
import cis5550.tools.Partitioner;
import cis5550.tools.Serializer;

import java.io.IOException;
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



    @Override
    public FlamePairRDD mapToPair(StringToPair lambda) throws Exception {
        byte[] serializedLambda = Serializer.objectToByteArray(lambda);
        FlameRDDImpl rdd = (FlameRDDImpl) ((FlameContextImpl) context).invokeOperation("/rdd/mapToPair", serializedLambda, this.tableName);
        return new FlamePairRDDImpl(rdd.tableName, rdd.context);
    }

    @Override
    public FlameRDD intersection(FlameRDD r) throws Exception {
        return null;
    }


    @Override
    public FlameRDD sample(double f) throws Exception {
        return null;
    }

    @Override
    public FlamePairRDD groupBy(StringToString lambda) throws Exception {
        return null;
    }
}
