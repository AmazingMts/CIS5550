package cis5550.flame;

import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.HTTP;
import cis5550.tools.Hasher;
import cis5550.tools.Partitioner;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cis5550.flame.Coordinator.kvs;

public class FlameContextImpl implements FlameContext {

    private String jarName;
    private List<String> outputs;
//    String tableName = "RDD_" + System.currentTimeMillis();
    public FlameContextImpl(String jarName) {
        this.jarName = jarName;
        outputs = new ArrayList<String>();

    }

    @Override
    public KVSClient getKVS() {
        return null;
    }

    @Override
    public void output(String s) {
        outputs.add(s);
    }
    @Override
    public FlameRDD parallelize(List<String> data) throws IOException {//use parallelize to transfer local data to distributed RDD
        String tableName = "RDD_" + System.currentTimeMillis();
        KVSClient kvs = new KVSClient("localhost:8000");
        for (int i = 0; i < data.size(); i++) {
            String rowKey = Hasher.hash("" + i);  // 对 i 进行哈希
            kvs.put(tableName, rowKey, "value", data.get(i));
        }

        return new FlameRDDImpl(tableName,this);
    }

    @Override
    public FlameRDD fromTable(String tableName, RowToString lambda) throws Exception {
        // 创建一个 KVS 客户端，用于扫描表
        KVSClient kvs = new KVSClient("localhost:8000");

        // 扫描指定表，并准备存储 lambda 函数返回的非空结果
        Iterator<Row> iterator = kvs.scan(tableName);
        List<String> results = new ArrayList<>();

        // 遍历表中的每一行数据
        while (iterator.hasNext()) {
            Row row = iterator.next();

            // 将行传递给 lambda 函数，并获取返回值
            String result = lambda.op(row);

            // 如果返回的结果不为空，将其添加到结果列表
            if (result != null) {
                results.add(result);
            }
        }

        // 使用 context.parallelize 将结果并行化并转换为新的 RDD
        return this.parallelize(results);
    }



    public List<String> getOutputs() {
        return outputs;
    }


    public Object invokeOperation(String operation, byte[] lambda, String inputTable,String... additionalParams) throws IOException {
        String outputTable = "output_" + System.currentTimeMillis();
        List<String> workerResults = new ArrayList<>();
        Partitioner partitioner = new Partitioner();
        partitioner.setKeyRangesPerWorker(1);
        kvs = new KVSClient("localhost:8000");
        int numKVSWorkers = kvs.numWorkers();
//        System.out.println("KVS 工人数: " + numKVSWorkers);

        // 为每个 KVS 工人添加分区
        if (numKVSWorkers > 0) {
            for (int i = 0; i < numKVSWorkers - 1; i++) {
                String workerAddress = kvs.getWorkerAddress(i);
                String startWorkerID = kvs.getWorkerID(i);
                String endWorkerID = kvs.getWorkerID(i + 1);
                partitioner.addKVSWorker(workerAddress, startWorkerID, endWorkerID);
//                System.out.println("添加 KVS 工人: " + workerAddress + ", 范围: " + startWorkerID + " - " + endWorkerID);
            }

            // 处理最后一个 KVS 工人，采用环状分配
            String lastWorkerAddress = kvs.getWorkerAddress(numKVSWorkers - 1);
            String lastWorkerID = kvs.getWorkerID(numKVSWorkers - 1);
            String firstWorkerID = kvs.getWorkerID(0);
            partitioner.addKVSWorker(lastWorkerAddress, lastWorkerID, null);  // 处理所有大于最后一个 ID 的键
            partitioner.addKVSWorker(lastWorkerAddress, null, firstWorkerID);  // 处理所有小于第一个 ID 的键
//            System.out.println("添加最后的 KVS 工人: " + lastWorkerAddress + ", 范围: " + lastWorkerID + " - null 和 null - " + firstWorkerID);
        }

        // 获取 Flame 工人列表
        List<String> flameWorkers = getFlameWorkers();
//        System.out.println("Flame 工人数: " + flameWorkers.size());

        // 为每个 Flame 工人添加分区
        for (String flameWorker : flameWorkers) {
            partitioner.addFlameWorker(flameWorker);
        }

        // 调用分区分配方法
        Vector<Partitioner.Partition> partitions = partitioner.assignPartitions();
//        for (Partitioner.Partition partition : partitions) {
//            System.out.println("分区范围: " + partition.fromKey + " 到 " + partition.toKeyExclusive + ", 被分配给 Flame 工人: " + partition.assignedFlameWorker);
//        }

        Thread[] threads = new Thread[partitions.size()];
        for (int i = 0; i < partitions.size(); i++) {
            final Partitioner.Partition partition = partitions.get(i);
            threads[i] = new Thread(() -> {
                try {
                    String flameWorkerURL = "http://" + partition.assignedFlameWorker + operation;
                    String body = "operation=" + URLEncoder.encode(operation, StandardCharsets.UTF_8.toString());
                    body += "&inputTable=" + URLEncoder.encode(inputTable, StandardCharsets.UTF_8.toString());
                    body += "&outputTable=" + URLEncoder.encode(outputTable, StandardCharsets.UTF_8.toString());
                    body += "&lambda=" + URLEncoder.encode(Base64.getEncoder().encodeToString(lambda), StandardCharsets.UTF_8.toString());

                    if (partition.fromKey != null) {
                        body += "&startKey=" + URLEncoder.encode(partition.fromKey, StandardCharsets.UTF_8.toString());
                    }

                    if (partition.toKeyExclusive != null) {
                        body += "&endKey=" + URLEncoder.encode(partition.toKeyExclusive, StandardCharsets.UTF_8.toString());
                    }
                    if (additionalParams.length > 0) {//deal with foldKey
                        if(operation.equals("/rdd/foldByKey")){
                        String zeroElement = additionalParams[0];

                        body += "&zeroElement=" + URLEncoder.encode(zeroElement, StandardCharsets.UTF_8);
                    }
                        else if(operation.equals("/rdd/intersection")){
                            String othertablename = additionalParams[0];

                            body += "&othertablename=" + URLEncoder.encode(othertablename, StandardCharsets.UTF_8);
                        }
                        else if(operation.equals("/rdd/sample")){
                            String f=additionalParams[0];
                            body += "&f=" + URLEncoder.encode(f, StandardCharsets.UTF_8);
                        }
                        else if(operation.equals("/pairRDD/join")){
                            String f=additionalParams[0];
                            body += "&otherTable=" + URLEncoder.encode(f, StandardCharsets.UTF_8);
                        }
                        else if(operation.equals("/pairRDD/fold")){
                            String zeroElement = additionalParams[0];
                            body+="&zeroElement=" + URLEncoder.encode(zeroElement, StandardCharsets.UTF_8);
                        }
                        else if(operation.equals("/rdd/mapPartitions")){
                            String f=additionalParams[0];
                            body += "&otherTable=" + URLEncoder.encode(f, StandardCharsets.UTF_8);
                        }
                    }
                    HTTP.Response httpResponse = HTTP.doRequest("POST", flameWorkerURL, body.getBytes(StandardCharsets.UTF_8));
                    String workerResult = new String(httpResponse.body(), StandardCharsets.UTF_8);
                    synchronized (workerResults) {
                        workerResults.add(workerResult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (operation.equals("/pairRDD/fold")){
            return workerResults;
        }
        return new FlameRDDImpl(outputTable,this);

    }
    public static List<String> getFlameWorkers() {
        return Coordinator.getWorkers();  // 返回一个 List<String>，其中每个字符串是 "IP:Port"
    }

}
