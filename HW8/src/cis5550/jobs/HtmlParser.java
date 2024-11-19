package cis5550.jobs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cis5550.tools.URLParser;
public class HtmlParser {

    public static Map<String, String> extractUrlsWithAnchors(String pageContent, String baseUrl) throws MalformedURLException {
        Map<String, String> urlsWithAnchors = new HashMap<>();
        Pattern tagPattern = Pattern.compile("<a\\s+([^>]*?)>(.*?)</a>", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(pageContent);
        Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);

        while (tagMatcher.find()) {
            String attributes = tagMatcher.group(1);
            String anchorText = tagMatcher.group(2).trim();
            Matcher hrefMatcher = hrefPattern.matcher(attributes);
            if (hrefMatcher.find()) {
                String url = hrefMatcher.group(1);
                String normalizedUrl = normalizeUrl(baseUrl, url);
                if (normalizedUrl != null) {
                    urlsWithAnchors.put(normalizedUrl, anchorText);
                }
            }
        }
        return urlsWithAnchors;
    }




    public static List<String> extractUrls(String pageContent, String baseUrl) throws MalformedURLException {
        List<String> urls = new ArrayList<>();
        Pattern tagPattern = Pattern.compile("<a\\s+([^>]*?)>", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(pageContent);
        Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);

        while (tagMatcher.find()) {
            String attributes = tagMatcher.group(1);
            Matcher hrefMatcher = hrefPattern.matcher(attributes);
            if (hrefMatcher.find()) {
                String url = hrefMatcher.group(1);
                String normalizedUrl=normalizeUrl(baseUrl,url);
                if (normalizedUrl!=null){
                    urls.add(normalizedUrl);
                }
            }
        }
        return urls;
    }
    //baseUrl是
    //link extracted by href
    private static String normalizeUrl(String baseUrl, String link) throws MalformedURLException {
        int hashIndex = link.indexOf("#");
        if (hashIndex != -1) {
            link = link.substring(0, hashIndex);
        }
        if (link.isEmpty()) {
            return baseUrl;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            link=Crawler.normalizeSeedUrl(link);
            URL absoluteUrl = new URL(link);
            return absoluteUrl.toString();
        }

        // 检查文件扩展名
        if (link.matches(".*\\.(jpg|jpeg|gif|png|txt)$")) {
            return null;  // 忽略特定扩展名
        }
        String[] baseParts = URLParser.parseURL(baseUrl);
        String baseProtocol = baseParts[0];
        String baseHost = baseParts[1];
        String basePort = baseParts[2] != null ? baseParts[2] : (baseProtocol.equals("https") ? "443" : "80");
        String basePath = baseParts[3];
        String[] linkParts = URLParser.parseURL(link);String linkProtocol = linkParts[0];
        if (linkProtocol!= null && (!linkProtocol.equals("http") && !linkProtocol.equals("https"))) {return null;}
        if (!link.startsWith("/")) {
            int lastSlash=basePath.lastIndexOf('/');
            if (lastSlash >=0) {
                basePath=basePath.substring(0,lastSlash+1);
            }
            while(link.startsWith("../")) {
                link=link.substring(3);
                int slashIndex=basePath.lastIndexOf('/',basePath.length()-2);
                if(slashIndex>=0) {
                    basePath=basePath.substring(0,slashIndex+1);
                }
            }
            link=basePath+link;
        }
        URL absoluteUrl = new URL(baseProtocol, baseHost, Integer.parseInt(basePort), link);
        String normalizedUrl = absoluteUrl.toString();
        if (!normalizedUrl.startsWith("http")) return null;
        if (normalizedUrl.endsWith(".jpg") || normalizedUrl.endsWith(".pdf") || normalizedUrl.endsWith(".png")) return null;
        return normalizedUrl;
    }
}
