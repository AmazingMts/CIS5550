package cis5550.test;

import cis5550.jobs.Crawler;

public class TestSeedURLNormalizer {
    public static void main(String[] args) {

        String[] seedUrls = {
                "https://foo.com/bar/xyz.html#section",       // 带有 # 片段，缺省端口
                "http://foo.com/bar",                         // 缺省端口
                "https://foo.com:8000/bar/xyz.html",          // 带有端口
                "http://foo.com:8080/bar#fragment",           // 带有端口和 # 片段
                "ftp://foo.com/resource",                     // 非 http/https 协议
                "https://example.com#top",                    // 缺省端口和 # 片段
                "https://foo.com/bar/page.jpg",               // 带有扩展名
                "http://foo.com:80/index.html",               // 已有默认端口号
                "https://foo.com:443/index.html#footer",      // https 的默认端口号和 # 片段
                "https://foo.com/"                            // 完整的种子 URL，无需更改
        };

        for (String seedUrl : seedUrls) {
            String normalizedUrl = Crawler.normalizeSeedUrl(seedUrl);
            System.out.println("原始 URL: " + seedUrl);
            System.out.println("规范化后的 URL: " + normalizedUrl);
            System.out.println("---------------------------");
        }
    }
}
