package cis5550.tools;

import java.util.Arrays;

public class TestURLParser {
    public static void main(String[] args) {
        // 输入测试 URL
        String testUrl = "/relative/path.html";

        // 调用 parseURL 方法
        String[] parsedResult = URLParser.parseURL(testUrl);

        // 打印解析结果
        System.out.println("Protocol: " + parsedResult[0]);
        System.out.println("Host: " + parsedResult[1]);
        System.out.println("Port: " + parsedResult[2]);
        System.out.println("Path: " + parsedResult[3]);
    }
}
