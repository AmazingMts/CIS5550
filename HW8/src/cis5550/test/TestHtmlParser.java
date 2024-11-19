package cis5550.test;

import cis5550.jobs.HtmlParser;
import java.util.List;

public class TestHtmlParser {
    public static void main(String[] args) {
        String baseUrl = "https://foo.com:8000/bar/xyz.html";
        String pageContent = """
                <html>
                    <body>
                        <a href="#abc">Anchor Link</a>
                        <a href="blah.html#test">Relative Link with Fragment</a>
                        <a href="../blubb/123.html">Parent Directory Link</a>
                        <a href="/one/two.html">Root Directory Link</a>
                        <a href="http://elsewhere.com/some.html">External Link with Protocol</a>
                        <a href="image.txt">Image File Link</a>
                    </body>
                </html>
                """;

        try {
            List<String> urls = HtmlParser.extractUrls(pageContent, baseUrl);
            System.out.println("Extracted and normalized URLs:");
            for (String url : urls) {
                System.out.println(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
