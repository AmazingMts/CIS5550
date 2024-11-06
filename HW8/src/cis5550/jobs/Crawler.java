package cis5550.jobs;
import cis5550.flame.*;
import cis5550.kvs.KVSClient;
import cis5550.kvs.Row;
import cis5550.tools.Hasher;
import cis5550.tools.URLParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Crawler {
    private static List<String> blacklistPatterns = new ArrayList<>();
    private static String wildcardToRegex(String pattern) {
        return pattern.replace(".", "\\.")
                .replace("?", "\\?")
                .replace("*", ".*");
    }
    private static boolean isBlacklisted(String url) {
        for (String pattern : blacklistPatterns) {
            if (url.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
    private static String computeHash(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(content);
        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            hashString.append(String.format("%02x", b));
        }
        return hashString.toString();
    }


    public static void run(FlameContext context,String[] args) throws Exception {
        if(args.length<1){
            context.output("invalid parameters, lack URL");
            return;
        }
        String seedurl = normalizeSeedUrl(args[0]);
        if (args.length > 1) {
            String blacklistTable = args[1];
            KVSClient kvs = new KVSClient("localhost:8000");
            for (Iterator<Row> it = kvs.scan(blacklistTable); it.hasNext(); ) {
                Row row = it.next();
                String pattern = row.get("pattern");
                if (pattern != null) {
                    blacklistPatterns.add(wildcardToRegex(pattern));
                }
            }
        }
        FlameRDD urlQueue=context.parallelize(List.of(seedurl));
        while(urlQueue.count()>0){
            urlQueue=urlQueue.flatMap(url->{
                List<String> extractedUrls = new ArrayList<>();//store extracted url
                try{
                    String urlHash = Hasher.hash(url);
                    URL currentURL=new URL(url);
                    KVSClient kvs = new KVSClient("localhost:8000");
                    String host = currentURL.getHost();

                    Map<String, String> robotsRules = loadRobotsRules(kvs, host);
                    if (!isAllowedByRobots(robotsRules, currentURL.getPath())) {
                        return extractedUrls;
                    }
                    if (isBlacklisted(url)) {
                        return extractedUrls;
                    }
                    if (kvs.existsRow("pt-crawl", urlHash)) {
                        return extractedUrls;
                    }
//
//                    long currentTime = System.currentTimeMillis();
//                    Row hostRow = kvs.getRow("hosts", host);
//
//                    if (hostRow != null) {
//                        String lastAccessTimeStr = hostRow.get("lastAccessTime");
//                        if (lastAccessTimeStr != null) {
//                            long lastAccessTime = Long.parseLong(lastAccessTimeStr);
//                            if (currentTime - lastAccessTime < 1000) {
//                                extractedUrls.add(url);
//                                return extractedUrls;
//                            }
//                        }
//                    }
//                    Row existingRow = kvs.getRow("hosts", host);
//                    if (existingRow == null) {
//                        existingRow = new Row(host);
//                    }
//
//                    existingRow.put("lastAccessTime", String.valueOf(currentTime));
//                    kvs.putRow("hosts", existingRow);


                    HttpURLConnection headConnection = (HttpURLConnection) currentURL.openConnection();
                    headConnection.setRequestMethod("HEAD");
                    headConnection.setRequestProperty("User-Agent", "cis5550-crawler");
                    headConnection.setInstanceFollowRedirects(false);

                    int headResponseCode = headConnection.getResponseCode();
                    String contentType = headConnection.getContentType();
                    String contentLength = headConnection.getHeaderField("Content-Length");

                    Row row = new Row(urlHash);
                    row.put("url", url);
                    row.put("responseCode", String.valueOf(headResponseCode));
                    if (contentType != null) row.put("contentType", contentType);
                    if (contentLength != null) row.put("length", contentLength);
                    if (headResponseCode == 301 || headResponseCode == 302 || headResponseCode == 303 || headResponseCode == 307 || headResponseCode == 308) {
                        String location = headConnection.getHeaderField("Location");

                        kvs.putRow("pt-crawl", row);
                        headConnection.disconnect();
                        if (location != null && !location.isEmpty()) {
                            URL newUrl = new URL(currentURL, location);
                            extractedUrls.add(newUrl.toString());
                        }
                        return extractedUrls;
                    }
                    else if (headResponseCode == 200 && contentType != null && contentType.startsWith("text/html")) {
                        HttpURLConnection connection = (HttpURLConnection) currentURL.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("User-Agent", "cis5550-crawler");
                        int getResponseCode = connection.getResponseCode();
                        row.put("responseCode", String.valueOf(getResponseCode));
                        if (connection.getResponseCode() == 200) {
                            InputStream inputStream = connection.getInputStream();
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            byte[] data = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(data)) != -1) {
                                buffer.write(data, 0, bytesRead);
                            }
                            byte[] pageContentBytes = buffer.toByteArray();
                            inputStream.close();
                            buffer.close();

                            String contentHash = computeHash(pageContentBytes);
                            Row contentRow = kvs.getRow("content-hashes", contentHash);
                            if (contentRow != null) {
                                row.put("canonicalURL", contentRow.get("url"));
                            } else {
                                row.put("page", pageContentBytes);
                                Row newContentRow = new Row(contentHash);
                                newContentRow.put("url", url);
                                kvs.putRow("content-hashes", newContentRow);

//                                Map<String, String> urlsWithAnchors = HtmlParser.extractUrlsWithAnchors(new String(pageContentBytes, StandardCharsets.UTF_8), seedurl);
//                                for (Map.Entry<String, String> entry : urlsWithAnchors.entrySet()) {
//                                    String anchorUrl = entry.getKey();
//                                    String anchorText = entry.getValue();
//
//                                    String anchorUrlHash = Hasher.hash(anchorUrl);
//                                    Row anchorRow = kvs.getRow("pt-crawl", anchorUrlHash);
//                                    if (anchorRow == null) {
//                                        anchorRow = new Row(anchorUrlHash);
//                                        anchorRow.put("url", anchorUrl);
//                                    }
//
//                                    // 使用列名前缀 anchor: 来存储锚文本
//                                    anchorRow.put("anchor:" + urlHash, anchorText);
//                                    kvs.putRow("pt-crawl", anchorRow);
//                                }
                                extractedUrls = HtmlParser.extractUrls(new String(pageContentBytes, StandardCharsets.UTF_8), seedurl);
                            }
                        }
                        connection.disconnect();
                    }
                    kvs.putRow("pt-crawl", row);
                    headConnection.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return extractedUrls;
            });
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    public static String normalizeSeedUrl(String seedUrl) {
        try {
            int hashIndex = seedUrl.indexOf('#');
            if (hashIndex != -1) {
                seedUrl = seedUrl.substring(0, hashIndex);
            }

            String[] parts = URLParser.parseURL(seedUrl);
            String protocol = parts[0];
            String host = parts[1];
            String port = parts[2];
            String path = parts[3];
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return null;
            }

            if (path != null && path.matches(".*\\.(jpg|jpeg|gif|png|txt)$")) {
                return null;
            }
            if (port == null) {
                port = protocol.equals("https") ? "443" : "80";
            }
            URL normalizedUrl = new URL(protocol, host, Integer.parseInt(port), path);
            return normalizedUrl.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
    private static Map<String, String> loadRobotsRules(KVSClient kvs, String host) throws IOException {
        Row hostRow = kvs.getRow("hosts", host);
        Map<String, String> robotsRules = new HashMap<>();
        if (hostRow != null && hostRow.get("robots") != null) {
            parseRobotsTxt(hostRow.get("robots"), robotsRules);
        } else {
            HttpURLConnection connection = (HttpURLConnection) new URL("http://" + host + "/robots.txt").openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "cis5550-crawler");
            if (connection.getResponseCode() == 200) {
                InputStream inputStream = connection.getInputStream();
                StringBuilder robotsContent = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    robotsContent.append(line).append("\n");
                }
                reader.close();
                connection.disconnect();
                Row existingRow = kvs.getRow("hosts", host);
                if (existingRow == null) {
                    existingRow = new Row(host); // 如果没有现有数据，则创建新的行
                }

                existingRow.put("robots", robotsContent.toString());
                kvs.putRow("hosts", existingRow);
                parseRobotsTxt(robotsContent.toString(), robotsRules);
            }
        }

        return robotsRules;
    }
    private static void parseRobotsTxt(String robotsContent, Map<String, String> robotsRules) {
        String[] lines = robotsContent.split("\n");
        boolean isRelevant = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("User-agent:")) {
                String agent = line.split(":")[1].trim();
                isRelevant = agent.equals("cis5550-crawler") || agent.equals("*");
            } else if (isRelevant) {
                if (line.startsWith("Disallow:")) {
                    String path = line.split(":")[1].trim();
                    robotsRules.put(path, "Disallow");
                } else if (line.startsWith("Allow:")) {
                    String path = line.split(":")[1].trim();
                    robotsRules.put(path, "Allow");
                }
            }
        }
    }

    // 检查 URL 路径是否被允许访问
    private static boolean isAllowedByRobots(Map<String, String> robotsRules, String path) {
        for (Map.Entry<String, String> entry : robotsRules.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue().equals("Allow");
            }
        }
        return true;
    }

}
