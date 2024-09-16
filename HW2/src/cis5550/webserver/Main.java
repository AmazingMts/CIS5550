package cis5550.webserver;

import cis5550.webserver.Server;

import static cis5550.webserver.Server.get;
import static cis5550.webserver.Server.put;

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
        get("/write", (req,res) -> { res.header("X-Bar", "present"); res.write("Hello ".getBytes()); res.write("World!".getBytes()); return null; });
        // 启动服务器
        Server.get("/pathpar/:val1/123/:val2", (req, res) -> {
            String val1 = req.params().get("val1");
            String val2 = req.params().get("val2");
            res.body("Val1: [" + val1 + "] Val2: [" + val2 + "]");
            return null;
        });
        Server.getInstance().get("/qparam", (req, res) -> {
            String par1 = req.queryParams("par1");
            String par2 = req.queryParams("par2");
            String par3 = req.queryParams("par3");
            String par4 = req.queryParams("par4");
            String responseBody = par1 + " " + par2 + "," + par3 + "," + par4;
            res.body(responseBody);
            return null;
        });

    }
}
