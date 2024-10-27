package cis5550.flame;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.*;
import java.io.*;

import static cis5550.webserver.Server.*;
import cis5550.tools.Hasher;
import cis5550.tools.Serializer;
import cis5550.kvs.*;

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


                Iterable<String> results = lambda.op(value);


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
                kvs.put(outputTable,"result" , columnKey, finalAccumulator.toString().getBytes(StandardCharsets.UTF_8));
            }

            response.body("Task completed. Data has been stored in KVS.");
            response.status(200, "successful");
            return "Task completed.";
        });
//        ------------------------------------------------------------------------
        post("/rdd/flatMapToPair", (request, response) -> {
            // 获取并解析 POST 请求的 body 内容
            String body = request.body();
            Map<String, String> params = new HashMap<>();

            // 将 body 中的参数按 "key=value" 格式解析
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // 获取参数
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");

            // 检查 lambda 参数是否为空
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 解码并反序列化 lambda
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToPairIterable lambda = (FlameRDD.StringToPairIterable) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable,startKey,endKey);

            // 遍历输入表中的每一行并应用 lambda
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Iterable<FlamePair> pairs = lambda.op(row.get("value")); // 调用 lambda 获取键值对

                for (FlamePair pair : pairs) {
                    String key = pair._1();
                    String value = pair._2();
                    String rowKey = UUID.randomUUID().toString(); // 唯一行键
                    kvs.put(outputTable, rowKey, key, value);     // key 列

                }
            }

            response.status(200, "successful");
            return "FlatMapToPair operation completed.";
        });


        post("/pairRDD/flatMap", (request, response) -> {
            // 获取并解析 POST 请求的 body 内容
            String body = request.body();
            Map<String, String> params = new HashMap<>();

            // 将 body 中的参数按 "key=value" 格式解析
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // 获取参数
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");

            // 检查 lambda 参数是否为空
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 解码并反序列化 lambda
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlamePairRDD.PairToStringIterable lambda =
                    (FlamePairRDD.PairToStringIterable) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);

            // 遍历 PairRDD 表中的每一行
            while (iterator.hasNext()) {
                Row row = iterator.next();

                // 遍历每个列，将列名作为原始键，列的值作为原始值
                for (String column : row.columns()) {
                    String originalKey = column;  // 使用列名作为原始 key
                    String originalValue = new String(row.get(column));  // 获取列的值

                    // 使用原始的键值对构造 FlamePair
                    FlamePair pair = new FlamePair(originalKey, originalValue);
                    Iterable<String> results = lambda.op(pair);

                    // 检查 lambda 结果是否为空，存储非空的字符串
                    if (results != null) {
                        for (String result : results) {
                            if (result != null) {
                                String uniqueRowKey = UUID.randomUUID().toString();
                                kvs.put(outputTable, uniqueRowKey, "value", result.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
            response.status(200, "successful");
            return "FlatMap operation on PairRDD completed.";
        });


        post("/pairRDD/flatMapToPair", (request, response) -> {
            // 获取并解析 POST 请求的 body 内容
            String body = request.body();
            Map<String, String> params = new HashMap<>();

            // 将 body 中的参数按 "key=value" 格式解析
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // 获取参数
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            String lambdaParam = params.get("lambda");

            // 检查 lambda 参数是否为空
            if (lambdaParam == null) {
                response.status(400, "Bad request");
                return "Missing 'lambda' parameter";
            }

            // 解码并反序列化 lambda
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlamePairRDD.PairToPairIterable lambda =
                    (FlamePairRDD.PairToPairIterable) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);

            // 遍历 PairRDD 表中的每一行
            while (iterator.hasNext()) {
                Row row = iterator.next();

                // 遍历每个列，将列名作为原始键，列的值作为原始值
                for (String column : row.columns()) {
                    String originalKey = column;  // 使用列名作为原始 key
                    String originalValue = new String(row.get(column));  // 获取列的值

                    // 构造 FlamePair 并应用 lambda
                    FlamePair pair = new FlamePair(originalKey, originalValue);
                    Iterable<FlamePair> results = lambda.op(pair);

                    // 检查结果并存储
                    if (results != null) {
                        for (FlamePair result : results) {
                            if (result != null) {
                                String uniqueRowKey = UUID.randomUUID().toString();
                                kvs.put(outputTable, uniqueRowKey, result._1(), result._2());
                            }
                        }
                    }
                }
            }

            response.status(200, "successful");
            return "FlatMapToPair operation on PairRDD completed.";
        });

        post("/rdd/distinct", (request, response) -> {
            // 解析请求中的 inputTable 和 outputTable 参数
            String body = request.body();
            Map<String, String> params = new HashMap<>();

            // 将 body 中的参数按 "key=value" 格式解析
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // 获取参数
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");

            // 扫描输入表，将唯一值插入输出表
            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable,startKey,endKey);

            while (iterator.hasNext()) {
                Row row = iterator.next();
                for (String column : row.columns()) {
                    String value = new String(row.get(column));
                    // 将值作为行键插入输出表，实现去重
                    kvs.put(outputTable, value, "value", value.getBytes(StandardCharsets.UTF_8));
                }
            }

            response.status(200, "successful");
            return "Distinct operation on RDD completed.";
        });


        post("/pairRDD/join", (request, response) -> {
            // 解析 HTTP 请求参数
            String body = request.body();
            Map<String, String> params = new HashMap<>();
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            String tableA = params.get("inputTable");
            String tableB = params.get("otherTable");
            String outputTable = params.get("outputTable");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iteratorA = kvs.scan(tableA,startKey,endKey);
            Iterator<Row> iteratorB = kvs.scan(tableB);

            while (iteratorA.hasNext()) {
                Row rowA = iteratorA.next();

                // 遍历表 B 的每一行
                while (iteratorB.hasNext()) {
                    Row rowB = iteratorB.next();

                    // 检查两个行是否有相同的列名
                    for (String columnA : rowA.columns()) {
                        if (rowB.columns().contains(columnA)) {
                            String valueA = rowA.get(columnA);
                            String valueB = rowB.get(columnA);

                            // 合并相同列的值
                            String combinedValue = valueA + "," + valueB;

                            // 使用行A和行B的键作为唯一键来存储结果
                            String uniqueKey = rowA.key() + "_" + rowB.key();
                            kvs.put(outputTable, uniqueKey, columnA, combinedValue.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                iteratorB = kvs.scan(tableB);
            }
            response.status(200, "successful");
            return "Join operation on PairRDD completed.";
        });



        post("/pairRDD/fold", (request, response) -> {
            // 解析 HTTP 请求参数
            String body = request.body();
            Map<String, String> params = new HashMap<>();
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // 获取参数
            String inputTable = params.get("inputTable");
            String zeroElement = params.get("zeroElement");
            String lambdaParam = params.get("lambda");
            String accumulator = zeroElement;
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");
            // 反序列化 lambda 函数
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlamePairRDD.TwoStringsToString lambda = (FlamePairRDD.TwoStringsToString) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable,startKey,endKey);

            while (iterator.hasNext()) {
                Row row = iterator.next();
                for (String column : row.columns()) {
                    String value = row.get(column);
                    accumulator = lambda.op(accumulator, value); // 逐次累加
                }
            }
            System.out.println(accumulator);
            System.out.println(accumulator.getClass().getName());
            response.body(accumulator);
            response.status(200, "successful");
            return accumulator;
        });

        post("/rdd/filter", (request, response) -> {
            // Parse the request body to retrieve parameters
            String body = request.body();
            Map<String, String> params = new HashMap<>();
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            // Get parameters from the parsed request
            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String lambdaParam = params.get("lambda");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");

            // Deserialize the lambda function
            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.StringToBoolean lambda = (FlameRDD.StringToBoolean) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable, startKey, endKey);

            while (iterator.hasNext()) {
                Row row = iterator.next();
                System.out.println("Row Key: " + row.key());

                // Retrieve and split the comma-separated values in the "value" column
                String value = row.get("value");
                String[] items = value.split(","); // Split by commas

                // Filter each item and store if it passes
                for (String item : items) {
                    System.out.println("Evaluating " + item);

                    // Check if item passes the filter
                    boolean passesFilter = lambda.op(item);
                    System.out.println("Evaluating " + item + " -> " + passesFilter);

                    if (passesFilter) {
                        // Store the filtered item with a unique key in the output table
                        String uniqueKey = row.key() + "_" + item; // Ensure unique key per item
                        kvs.put(outputTable, uniqueKey, "value", item);
                    }
                }
            }

            response.status(200, "Filter operation completed");
            return outputTable;
        });


        post("/rdd/mapPartitions", (request, response) -> {

            String body = request.body();
            Map<String, String> params = new HashMap<>();
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            String inputTable = params.get("inputTable");
            String outputTable = params.get("outputTable");
            String lambdaParam = params.get("lambda");
            String startKey = params.get("startKey");
            String endKey = params.get("endKey");

            byte[] lambdaBytes = Base64.getDecoder().decode(lambdaParam);
            FlameRDD.IteratorToIterator lambda = (FlameRDD.IteratorToIterator) Serializer.byteArrayToObject(lambdaBytes, myJAR);

            KVSClient kvs = new KVSClient("localhost:8000");
            Iterator<Row> iterator = kvs.scan(inputTable,startKey,endKey);

            List<String> partitionData = new ArrayList<>();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String value = row.get("value");
                partitionData.add(value);
            }

            Iterator<String> resultIterator = lambda.op(partitionData.iterator());
            int i = 0;
            while (resultIterator.hasNext()) {
                String result = resultIterator.next();
//                System.out.println(result);
                String rowKey = "row_" + System.currentTimeMillis() + "_" + i++;
                kvs.put(outputTable, rowKey, "value", result);
            }
            response.status(200, "MapPartitions operation completed");
            return outputTable;
        });


        post("/pairRDD/cogroup", (request, response) -> {
            // 解析请求中的参数
            String body = request.body();
            Map<String, String> params = new HashMap<>();
            String[] pairsArray = body.split("&");
            for (String pair : pairsArray) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            }

            String inputTable = params.get("inputTable");
            String otherTable = params.get("otherTable");
            String outputTable = params.get("outputTable");

            KVSClient kvs = new KVSClient("localhost:8000");

            // 读取第一个 RDD 表的数据
            Map<String, List<String>> thisMap = new HashMap<>();
            Iterator<Row> thisIterator = kvs.scan(inputTable);
            while (thisIterator.hasNext()) {
                Row row = thisIterator.next();
                String key = row.key();
                List<String> values = new ArrayList<>();
                for (String col : row.columns()) {
                    values.add(row.get(col));
                }
                thisMap.put(key, values);
            }

            // 读取第二个 RDD 表的数据
            Map<String, List<String>> otherMap = new HashMap<>();
            Iterator<Row> otherIterator = kvs.scan(otherTable);
            while (otherIterator.hasNext()) {
                Row row = otherIterator.next();
                String key = row.key();
                List<String> values = new ArrayList<>();
                for (String col : row.columns()) {
                    values.add(row.get(col));
                }
                otherMap.put(key, values);
            }

            // 对所有键进行聚合，生成 [X],[Y] 格式的结果
            Set<String> allKeys = new HashSet<>(thisMap.keySet());
            allKeys.addAll(otherMap.keySet());

            for (String key : allKeys) {
                List<String> thisValues = thisMap.getOrDefault(key, new ArrayList<>());
                List<String> otherValues = otherMap.getOrDefault(key, new ArrayList<>());

                String resultValue = "[" + String.join(",", thisValues) + "],[" + String.join(",", otherValues) + "]";
                kvs.put(outputTable, key, "value", resultValue);
            }

            response.status(200, "cogroup operation completed");
            return outputTable;
        });













    }
}
