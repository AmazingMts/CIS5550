package cis5550.jobs;

import cis5550.flame.FlameContext;
import cis5550.flame.FlamePair;
import cis5550.flame.FlamePairRDD;
import cis5550.flame.FlameRDD;
import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.Hasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PageRank {
    public static void run(FlameContext ctx, String[] args) throws Exception {
        KVSClient kvs = new KVSClient("localhost:8000");

        // Step 1: Load the data from the "pt-crawl" table
        FlameRDD rdd = ctx.fromTable("pt-crawl", row -> {
            String url = row.get("url");
            String page = row.get("page");
            return url + ',' + page;
        });

        // Step 2: Create (urlHash, "1.0,1.0,links") pairs for the initial state
        FlamePairRDD prdd = rdd.mapToPair(s -> {
            String[] parts = s.split(",", 2);
            String url = parts[0];
            String urlHash = Hasher.hash(url);
            String page = parts[1];
            List<String> Hashurls = new ArrayList<>();
            List<String> urls = HtmlParser.extractUrls(page, url);

            // 使用 Set 去重，并排除自连接
            Set<String> uniqueUrls = new HashSet<>();
            for (String extractedUrl : urls) {

                    uniqueUrls.add(extractedUrl);

            }
            // 对每个唯一链接计算哈希值
            for (String uniqueUrl : uniqueUrls) {
                Hashurls.add(Hasher.hash(uniqueUrl));
            }
            String L = String.join(",", Hashurls);
            return new FlamePair(urlHash, "1.0,1.0," + L);
        });
        prdd.collect().forEach(pair -> {
            String urlHash = pair._1();
            String value = pair._2();
            try {
                kvs.put("pt-pageranks", urlHash, "rank", value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });



//
//
        // Step 3: Iterate until convergence
        double convergenceThreshold = Double.parseDouble(args[0]);
        FlamePairRDD updatedStateTable = null;
        boolean converged = false;
        FlamePairRDD currentRDD = prdd;
        int iterationnum=1;
        double requiredPercentage = args.length > 1 ? Double.parseDouble(args[1]) : 100.0;
        while (!converged) {

//            Iterator<Row> rows = kvs.scan("pt-pageranks");
//            while (rows.hasNext()) {
//                Row row = rows.next();
//                System.out.println("新表是"+"Row Key: " + row.key());
//                for (String column : row.columns()) {
//                    String value = row.get(column);
//                    System.out.println("Column: " + column + " -> Value: " + value);
//                }
//            }


            // Step 4: Compute the transfer table
            FlamePairRDD transferTable = currentRDD.flatMapToPair(pair -> {
                String urlHash = pair._1();
                String[] values = pair._2().split(",", 3);
                double rc = Double.parseDouble(values[0]);
                //这里有问题，rc一直1.0
//                System.out.println("TRANSFER内部"+urlHash+rc);
                String[] links = values[2].isEmpty() ? new String[0] : values[2].split(",");

                List<FlamePair> results = new ArrayList<>();
                if (links.length > 0) {
                    double transferValue = 0.85 * rc / links.length;
                    for (String linkHash : links) {
                        results.add(new FlamePair(linkHash, String.valueOf(transferValue)));
                    }
                }
                // 处理没有入度的页面，加入一个初始值 0.0 来确保它们的值也能被聚合
                results.add(new FlamePair(urlHash, "0.0"));
//                results.forEach(pair1 -> System.out.println(pair1._1() + ": " + pair1._2()));
                return results;
            });
//
//            transferTable.collect().forEach(pair -> {
//                System.out.println("Transfer Table - Key: " + pair._1() + ", Value: " + pair._2());
//            });
            // Step 5: Aggregate the transfers
            FlamePairRDD aggregatedRanks = transferTable.foldByKey("0.0", (v1, v2) -> {
                double accumulatedValue = Double.parseDouble(v1);
                double currentValue = Double.parseDouble(v2);

                // Add the current value to the accumulated value
                double newValue = accumulatedValue + currentValue;

                // Return the new accumulated value as a string
                return Double.toString(newValue);
            });
//            aggregatedRanks.collect().forEach(pair -> System.out.println("aggregate"+pair._1() + ": " + pair._2()));
//
////
//            // Step 6: Load the old state table
            FlameRDD rdd2 = ctx.fromTable("pt-pageranks", row -> {
                String urlHash = row.key();
                String state = row.get("rank");
                return urlHash + "," + state;
            });

            FlamePairRDD oldStateTable = rdd2.mapToPair(s -> {
                String[] parts = s.split(",", 2);
                String urlHash = parts[0];
                String state = parts[1];
                return new FlamePair(urlHash, state);
            });

            // Step 7: Join old state table and aggregated ranks
            FlamePairRDD joinedTable = oldStateTable.join(aggregatedRanks);

            // Step 8: Update the state table
            updatedStateTable = joinedTable.flatMapToPair(pair -> {
                String urlHash = pair._1();
                String[] values = pair._2().split(",");

                double newAggregatedRank = Double.parseDouble(values[values.length - 1]);
                double oldCurrentRank = Double.parseDouble(values[0]);

                String[] links = Arrays.copyOfRange(values, 2, values.length - 1);
                String linksString = (links.length > 0) ? String.join(",", links) : "";


                double updatedRank = 0.15 + newAggregatedRank;

                // 更新后的状态
                String updatedState = updatedRank + "," + oldCurrentRank + "," + linksString;
                return Collections.singleton(new FlamePair(urlHash, updatedState));
            });
            currentRDD = updatedStateTable;
            // Save the updated state
            updatedStateTable.collect().forEach(pair -> {
                String urlHash = pair._1();
                String value = pair._2();
                try {
//                    System.out.println("旧一轮储存的值"+urlHash + "," + value);
                    kvs.put("pt-pageranks", urlHash, "rank", value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Check for convergence
            FlameRDD convergenceCheck = updatedStateTable.flatMap(pair -> {
                String[] parts = pair._2().split(",");
                double oldRank = Double.parseDouble(parts[1]);
                double newRank = Double.parseDouble(parts[0]);
                double change = Math.abs(newRank - oldRank);
//                System.out.println("URL Hash: " + pair._1() + " -> Change: " + change);
                return Collections.singleton(change <= convergenceThreshold ? "1" : "0");
            });
            double maxChange = Double.parseDouble(convergenceCheck.fold("0.0", (v1, v2) -> {
                double value1 = Double.parseDouble(v1);
                double value2 = Double.parseDouble(v2);
                double maxValue=value1+value2;

                // 输出每次比较的结果
//                System.out.println("Comparing: " + value1 + " and " + value2 + " -> Max: " + maxValue);

                return String.valueOf(maxValue);
            }));
//            System.out.println("累加值"+maxChange);
            long totalUrls = updatedStateTable.collect().size();
//            System.out.println("总共个数"+totalUrls);
            double percentageWithinThreshold = (maxChange*100) / totalUrls;
            Boolean hasConverged=percentageWithinThreshold>=requiredPercentage;
            if (hasConverged) {
                converged = true;
            }
        }

        // Step 9: Save the final results
        updatedStateTable.collect().forEach(pair -> {
            String urlHash = pair._1();
            String[] values = pair._2().split(",");
            String finalRank = values[0];
            try {
                kvs.put("pt-pageranks", urlHash, "rank", finalRank);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
