package cis5550.flame;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.*;
import java.io.*;

import static cis5550.webserver.Server.*;
import cis5550.tools.Hasher;
import cis5550.tools.Serializer;
import cis5550.kvs.*;
import cis5550.webserver.Request;

class Worker extends cis5550.generic.Worker {

	public static void main(String args[]) {
    if (args.length != 2) {
    	System.err.println("Syntax: Worker <port> <coordinatorIP:port>");
    	System.exit(1);
    }

    int port = Integer.parseInt(args[0]);
    String server = args[1];
	  startPingThread(server, ""+port, port);
    final File myJAR = new File("__worker"+port+"-current.jar");

  	port(port);

    post("/useJAR", (request,response) -> {
      FileOutputStream fos = new FileOutputStream(myJAR);
      fos.write(request.bodyAsBytes());
      fos.close();
      response.body("Task completed. Data has been stored in KVS.");
      return "Task completed.";
    });
    post("/rdd/flatMap", (request, response) -> {
            File jarfile=new File("");
            // 解析HTTP请求中的参数
            String body = request.body(); // 获取POST请求的body内容
            Map<String, String> params = new HashMap<>();

            // 将body中的参数按 "key=value" 的格式解析
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }
            // 从解析好的参数Map中获取具体的参数值
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");

            // 检查 lambda 参数
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 反序列化 lambda 参数
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToIterable lambda = (FlameRDD.StringToIterable) Serializer.byteArrayToObject(lambdaBytes,jarfile);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);  // 获取迭代器

            // 创建一个 List 来存储所有的结果
            List<String> allResults = new ArrayList<>();

            // 遍历每一行并应用lambda操作
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value");  // 获取当前行的值
                System.out.println(value);

                // 调用lambda对当前行的值进行处理
                Iterable<String> results = lambda.op(value);

                // 将结果加入到 allResults 列表中
                for (String result : results) {
                    allResults.add(result);
                }
            }

            // 对所有结果进行排序
            Collections.sort(allResults);

            // 将排序后的结果写入输出表
            for (String result : allResults) {
                // 使用哈希生成唯一行键，确保唯一性
                String uniqueRowKey = Hasher.hash(result + UUID.randomUUID().toString());
                kvs.put(outputTable, uniqueRowKey, "value", result);  // 写入输出表
            }

            // 返回成功状态
            response.status(200, "successful");
            response.body("Task completed. Data has been stored in KVS.");
            return "Task completed.";
        });
        post("/rdd/mapToPair", (request, response) -> {
            File jarfile=new File("");

            // 解析HTTP请求中的参数
            String body = request.body(); // 获取POST请求的body内容
            Map<String, String> params = new HashMap<>();

            // 将body中的参数按 "key=value" 的格式解析
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");

            // 检查 lambda 参数
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 反序列化 lambda 参数
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToPair lambda = (FlameRDD.StringToPair) Serializer.byteArrayToObject(lambdaBytes, jarfile);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);  // 获取迭代器

            // 遍历每一行并应用lambda的 op 方法
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value");  // 假设当前行的值存储在 "value" 列中
                FlamePair pair = lambda.op(value);  // 使用 op 方法
                if (pair != null) {
                    kvs.put(outputTable, row.key(),pair._1(), pair._2().getBytes(StandardCharsets.UTF_8));
                }
            }

            // 返回成功状态
            response.status(200, "successful");
            response.body("Task completed. Data has been stored in KVS.");
            return "Task completed.";


        });
        post("/rdd/foldByKey", (request, response) -> {
            File jarfile = new File("");

            // 解析HTTP请求中的参数
            String body = request.body(); // 获取POST请求的body内容
            Map<String, String> params = new HashMap<>();

            // 将body中的参数按 "key=value" 的格式解析
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");
            String zeroElement = params.get("zeroElement");

            System.out.println(zeroElement);

            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlamePairRDD.TwoStringsToString lambda = (FlamePairRDD.TwoStringsToString) Serializer.byteArrayToObject(lambdaBytes, jarfile);
            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable);  // 扫描输入表

            // 全局累加器Map：用于存储每个列名的总和
            Map<String, Integer> globalAccumulator = new HashMap<>();

            // 遍历输入表的每一行
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String rowKey = row.key();  // 获取当前行的 key

                // 遍历当前行的所有列
                Set<String> columnKeys = row.columns();
                for (String columnKey : columnKeys) {
                    String value = row.get(columnKey);  // 获取当前列的值

                    Integer accumulator = globalAccumulator.getOrDefault(columnKey, Integer.parseInt(zeroElement));

                    // 执行累加操作
                    try {
                        accumulator += Integer.parseInt(value);  // 累加列的值
                    } catch (NumberFormatException e) {
                        System.err.println("无法解析的数值：" + value);
                        continue;
                    }

                    globalAccumulator.put(columnKey, accumulator);
                }
            }

            // 将所有累加结果写入输出表
            for (Map.Entry<String, Integer> entry : globalAccumulator.entrySet()) {
                String columnKey = entry.getKey();
                Integer finalAccumulator = entry.getValue();
                System.out.println("写入输出表: 列名 = " + columnKey + ", 累加值 = " + finalAccumulator);
                kvs.put(outputTable,"result" , columnKey, finalAccumulator.toString().getBytes(StandardCharsets.UTF_8));
            }

            response.body("Task completed. Data has been stored in KVS.");
            response.status(200, "successful");
            return "Task completed.";
        });
    }
}
