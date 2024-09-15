package cis5550.webserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {
        Server.port(8080);

        Server.post("/gettestfile", (req, res) -> {
            File file = new File("/Users/mts/CIS5550/HW2/src/test.html");
            if (file.exists() && !file.isDirectory()) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    res.body(content);
                    return res.getBody();
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Error reading file.";
                }
            } else {
                return "File not found.";
            }
        });


        ;

        Server.getInstance(); // 启动服务器
    }
}
