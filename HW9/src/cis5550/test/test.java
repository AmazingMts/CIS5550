//package cis5550.test;
//
//import cis5550.flame.FlamePair;
//import cis5550.flame.FlamePairRDD;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class PageRankTest {
//
//    public static void main(String[] args) throws Exception {
//        // 创建一个简单的传递表
//        List<FlamePair> pages = new ArrayList<>();
//        // 假设页面 A，PageRank 初始值为 1.0，链接到 B 和 C
//        pages.add(new FlamePair("A", "1.0,0.0,B,C"));
//        // 假设页面 B，PageRank 初始值为 1.0，没有出链
//        pages.add(new FlamePair("B", "1.0,0.0"));
//        // 假设页面 C，PageRank 初始值为 1.0，链接到 A
//        pages.add(new FlamePair("C", "1.0,0.0,A"));
//
//        // 转换为 FlamePairRDD
//        FlamePairRDD prdd = new FlamePairRDD(pages);
//
//        // 运行 PageRank 计算
//        FlamePairRDD transferTable = prdd.flatMapToPair(pair -> {
//            String urlHash = pair._1();
//            String[] values = pair._2().split(",");
//            double rc = Double.parseDouble(values[0]);
//            String[] links = Arrays.copyOfRange(values, 2, values.length);
//
//            List<FlamePair> results = new ArrayList<>();
//            if (links.length > 0) {
//                double transferValue = 0.85 * rc / links.length;
//                for (String linkHash : links) {
//                    results.add(new FlamePair(linkHash, String.valueOf(transferValue)));
//                }
//            } else {
//                results.add(new FlamePair(urlHash, String.valueOf(0.85 * rc)));
//            }
//            results.add(new FlamePair(urlHash, "0.0"));
//            return results;
//        });
//
//        // 输出结果，查看 PageRank 的传递值
//        transferTable.collect().forEach(pair -> {
//            System.out.println(pair._1() + " -> " + pair._2());
//        });
//
//        // 聚合同一节点的传递值
//        FlamePairRDD aggregatedRanks = transferTable.foldByKey("0.0", (v1, v2) -> {
//            double sum = Double.parseDouble(v1) + Double.parseDouble(v2);
//            return String.valueOf(sum);
//        });
//
//        // 输出聚合后的 PageRank 值
//        aggregatedRanks.collect().forEach(pair -> {
//            System.out.println("Node " + pair._1() + " new rank: " + pair._2());
//        });
//    }
//}
