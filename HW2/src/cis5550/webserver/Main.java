package cis5550.webserver;

import cis5550.webserver.Server;

public class Main {
    public static void main(String[] args) {
        // 设置服务器端口和静态文件目录
        Server.port(10000);
        Server.staticFiles.location("/path/to/static/files");

        // 配置GET路由
        Server.get("/test", (req, res) -> {
            return req.url();
        });
        Server.post("/hello", (req, res) -> {
            return "Request body: " + req.body();
        });

        // 简单的POST路由，返回请求体内容
        Server.post("/postbody", (req, res) -> {
            return "Request body: " + req.body(); // 读取并返回请求体
        });

        // 启动服务器
        Server.getInstance().run();
    }
}
