package cis5550.jobs;

import cis5550.flame.FlameContext;
import cis5550.flame.FlamePair;
import cis5550.flame.FlamePairRDD;
import cis5550.flame.FlameRDD;
import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Indexer {
    public static void run(FlameContext ctx, String[] args) throws Exception {
        KVSClient kvs = new KVSClient("localhost:8000");

        // Load data from the pt-crawl table into an RDD
        FlameRDD rdd = ctx.fromTable("pt-crawl", row -> {
            String url = row.get("url");
            String page = row.get("page");
            return url + "," + page;
        });

        // Convert RDD to a PairRDD of (url, page)
        FlamePairRDD pairRDD = rdd.mapToPair(s -> {
            String[] parts = s.split(",", 2);
            String url = parts[0];
            String page = parts[1];
            return new FlamePair(url, page);
        });
        // Create (word, url) pairs from (url, page) pairs
        FlamePairRDD invertedIndex = pairRDD.flatMapToPair(pair -> {
            String url = pair._1();
            String cleanedPage = pair._2().toLowerCase()
                    .replaceAll("<[^>]*>", " ") // 移除HTML标签
                    .replaceAll("[^a-zA-Z0-9\\s]", " ") // 移除标点符号
                    .replaceAll("[\\r\\n\\t]", " ") // 移除CR, LF, 和tab字符
                    .replaceAll("\\s+", " "); // 合并多个空格为一个空格

            String[] words = cleanedPage.split(" ");
            Map<String, List<Integer>> wordPositions = new HashMap<>();

            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (!word.isEmpty()) {
                    wordPositions.computeIfAbsent(word, k -> new ArrayList<>()).add(i + 1); // 位置从1开始
                }
            }

            List<FlamePair> results = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> entry : wordPositions.entrySet()) {
                String word = entry.getKey();
                List<Integer> positions = entry.getValue();
                if (!word.isEmpty()) {
                    // 将位置列表转换为用空格分隔的字符串
                    String positionsString = positions.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(" "));
                    // 格式为 "url:positions"
                    results.add(new FlamePair(word, url + ":" + positionsString));
                }
            }
            return results;
        });

        FlamePairRDD indexRDD = invertedIndex.foldByKey("", (existingUrls, newUrl) -> {
            // Split the existing URLs into a list for easier sorting
            List<String> urls = new ArrayList<>();
            if (!existingUrls.isEmpty()) {
                urls.addAll(Arrays.asList(existingUrls.split(",")));
            }

            // Add the new URL if it’s not already in the list
            if (!urls.contains(newUrl)) {
                urls.add(newUrl);
            }

            // Sort URLs based on the count of positions after the last ":"
            urls.sort((a, b) -> {
                // Extract the positions string after the last ":" in each URL
                String positionsA = a.substring(a.lastIndexOf(":") + 1);
                String positionsB = b.substring(b.lastIndexOf(":") + 1);

                // Count the number of positions in each
                int countA = positionsA.split(" ").length;
                int countB = positionsB.split(" ").length;

                // Sort in descending order based on the position counts
                return Integer.compare(countB, countA);
            });

            // Join the sorted URLs back into a comma-separated string
            return String.join(",", urls);
        });

        indexRDD.saveAsTable("pt-index");
    }
}
