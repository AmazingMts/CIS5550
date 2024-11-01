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
            System.out.println(startKey);
            System.out.println(endKey);
            // 检查 lambda 参数
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 反序列化 lambda 参数
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToIterable lambda = (FlameRDD.StringToIterable) Serializer.byteArrayToObject(lambdaBytes,myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");

            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);  // 获取迭代器

            // 创建一个 List 来存储所有的结果
            List<String> allResults = new ArrayList<>();

            // 遍历每一行并应用lambda操作
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value");  // 获取当前行的值

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
            File jarFile = new File("/Users/mts/Desktop/HW6/tests/flame-maptopair.jar");
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
            FlameRDD.StringToPair lambda = (FlameRDD.StringToPair) Serializer.byteArrayToObject(lambdaBytes, myJAR);

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
            File jarfile = new File("/Users/mts/Desktop/HW6/tests/flame-foldbykey.jar");

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

            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlamePairRDD.TwoStringsToString lambda = (FlamePairRDD.TwoStringsToString) Serializer.byteArrayToObject(lambdaBytes, myJAR);
            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable,startKey,endKey);  // 扫描输入表

            Map<String, String> globalAccumulator = new HashMap<>();

            // 遍历输入表的每一行
            while (iterator.hasNext()) {
                Row row = iterator.next();
                // 遍历当前行的所有列
                Set<String> columnKeys = row.columns();
                for (String columnKey : columnKeys) {
                    String value = row.get(columnKey);  // 获取当前列的值

                    String accumulator = globalAccumulator.getOrDefault(columnKey, zeroElement);

                    // 执行累加操作
                    try {
                        accumulator = lambda.op(accumulator, value);  // 累加列的值
                    } catch (NumberFormatException e) {
                        System.err.println("无法解析的数值：" + value);
                        continue;
                    }

                    globalAccumulator.put(columnKey, accumulator);
                }
            }

            // 将所有累加结果写入输出表
            for (Map.Entry<String, String> entry : globalAccumulator.entrySet()) {
                String columnKey = entry.getKey();
                String finalAccumulator = entry.getValue();
//                byte[] existingValueBytes = kvs.get(outputTable, "result", columnKey);
//                String existingValue = (existingValueBytes != null && existingValueBytes.length > 0)
//                        ? new String(existingValueBytes, StandardCharsets.UTF_8)
//                        : null;
//                System.out.println(existingValue);
//                if (existingValue != null) {
//                    System.out.println(existingValue);
//                    finalAccumulator = lambda.op(existingValue, finalAccumulator);
//                }
                System.out.println("写入输出表: 列名 = " + columnKey + ", 累加值 = " + finalAccumulator);
                String key = UUID.randomUUID().toString();
                kvs.put(outputTable,key , columnKey, finalAccumulator.toString().getBytes(StandardCharsets.UTF_8));
            }

            response.body("Task completed. Data has been stored in KVS.");
            response.status(200, "successful");
            return "Task completed.";
        });


        post("/rdd/intersection", (request, response) -> {
            // Parse the HTTP request body
            String body = request.body(); // Get the POST request body content
            Map<String, String> params = new HashMap<>();

            // Split the body content into key-value pairs
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // Extract table names from the request parameters
            String currentTableName = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String otherTableName = params.get("othertablename");

            // Ensure both table names are provided
            if (currentTableName == null || otherTableName == null) {
                response.status(400, "Bad request");
                System.out.println("Bad request");
                return "Missing 'tableName' or 'othertablename' parameter";
            }

            // Initialize the KVS client to interact with the key-value store
            KVSClient kvs = new KVSClient("localhost:8000");

            // Scan the current RDD table and collect its values in a Set
            Set<String> currentRDDSet = new HashSet<>();
            Iterator<Row> currentIterator = kvs.scan(currentTableName, startKey, endKey);
            while (currentIterator.hasNext()) {
                Row row = currentIterator.next();
                currentRDDSet.add(row.get("value").trim());  // 确保值没有多余空格
            }

            Set<String> otherRDDSet = new HashSet<>();
            Iterator<Row> otherIterator = kvs.scan(otherTableName);
            while (otherIterator.hasNext()) {
                Row row = otherIterator.next();
                otherRDDSet.add(row.get("value").trim());  // 确保值没有多余空格
            }

            currentRDDSet.retainAll(otherRDDSet);
            System.out.println("Intersection Result = " + currentRDDSet);

            Set<String> storedValues = new HashSet<>();  // 用于检查已经存储的值

            for (String value : currentRDDSet) {
                if (!storedValues.contains(value)) {  // 检查是否已经插入该值
                    String key = UUID.randomUUID().toString();  // 生成唯一的键
                    kvs.put(outputTable, key, "value", value.getBytes(StandardCharsets.UTF_8));  // 存储唯一值
                    storedValues.add(value);  // 将插入的值加入存储的值集合中
                }
            }
            System.out.println("Intersection stored in table: " + outputTable);

            Iterator<Row> outputIterator = kvs.scan(outputTable);
            System.out.println("Contents of " + outputTable + ":");
            while (outputIterator.hasNext()) {
                Row row = outputIterator.next();
                String value = row.get("value");
                System.out.println("Key: " + row.key() + ", Value: " + value);
            }

// 返回输出表的名称
            response.status(200, "successful");
            response.body("Intersection task completed. Result stored in table: " + outputTable);
            return outputTable;
        });



        post("/rdd/sample", (request, response) -> {
            String body = request.body(); // Get the POST request body content
            Map<String, String> params = new HashMap<>();

            // Split the body content into key-value pairs
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // Extract table names from the request parameters
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            double f = Double.parseDouble(params.get("f"));
            System.out.println("StartKey: " +startKey);
            System.out.println("endKey: " +endKey);

            // 初始化 KVS 客户端
            KVSClient kvs = new KVSClient("localhost:8000");

            // 使用 startKey 和 endKey 进行范围扫描
            Iterator<Row> iterator;
            iterator = kvs.scan(inputTable, startKey, endKey); // 只扫描 startKey 到 endKey 的数据

            // 遍历当前表，随机采样每个元素
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value").trim();

                // 以 f 的概率采样每个元素
                if (Math.random() < f) {
                    String key = UUID.randomUUID().toString(); // 生成唯一键
                    kvs.put(outputTable, key, "value", value.getBytes(StandardCharsets.UTF_8)); // 存储采样值
                }
            }

            // 返回输出表的名字
            response.status(200, "successful");
            return outputTable;
        });


        post("/rdd/groupBy", (request, response) -> {
            // 解析 HTTP 请求的 body

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
            System.out.println(startKey);
            System.out.println(endKey);
            // 检查 lambda 参数
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 反序列化 lambda 参数
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToString lambda = (FlameRDD.StringToString) Serializer.byteArrayToObject(lambdaBytes,myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable);
            Map<String, List<String>> groupedData = new HashMap<>();

            // 遍历每一行并应用 lambda 进行分组
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value").trim();
                String key = lambda.op(value);

                // 根据 key 将 value 分组
                groupedData.putIfAbsent(key, new ArrayList<>());
                groupedData.get(key).add(value);
            }

            // 创建新表用于存储结果
            for (Map.Entry<String, List<String>> entry : groupedData.entrySet()) {
                String key = entry.getKey();
                String values = String.join(",", entry.getValue());  // 将值拼接为逗号分隔的字符串
                System.out.println(values);
                kvs.put(outputTable, key, key, values.getBytes(StandardCharsets.UTF_8));
            }

            response.status(200, "successful");
            return outputTable;
        });

        post("/rdddd/groupBy", (request, response) -> {
            return "你好啊";
        });


    }
}
