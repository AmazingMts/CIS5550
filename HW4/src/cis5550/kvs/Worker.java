package cis5550.kvs;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import cis5550.webserver.Server;

import static cis5550.webserver.Server.*;

public class Worker {
    private static Map<String, Map<String, Row>> dataStore = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("bad request");
            return;
        }
        String PortNumber=args[0];
        String directory=args[1];
        String[] coordnet=args[2].split(":");
        String IP=coordnet[0];
        int Port=Integer.parseInt(coordnet[1]);
        Server.port(Integer.parseInt(PortNumber));
        File direct = new File(directory);
        if (!direct.exists()) {
            direct.mkdirs();
        }
        File file=new File(directory,"id");
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(5000);
                    String workerId=readfromFile(file);
                    sendpingrequest(workerId,IP,PortNumber,Port);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        put("/data/:table/:row/:column", (req, res) -> {
            String tableName = req.params("table");  // 获取表名
            String rowKey = req.params("row");       // 获取行名
            String columnKey = req.params("column"); // 获取列名
            byte[] data = req.bodyAsBytes();         // 获取请求体中的数据（可能是二进制）
            String ifcolumn = req.queryParams("ifcolumn");
            String equalsValue = req.queryParams("equals");
            if (ifcolumn != null && equalsValue != null) {
                // 获取行数据
                Map<String, Row> table = dataStore.get(tableName);
                if (table != null && table.containsKey(rowKey)) {
                    Row row = table.get(rowKey);

                    // 检查 ifcolumn 列是否存在，并且值是否匹配 equalsValue
                    byte[] columnValueBytes = row.get(ifcolumn);
                    if (columnValueBytes == null) {
                        res.status(409, "Column name Not Found");
                        return "FAIL";
                    }
                    String columnValueStr = new String(columnValueBytes, "UTF-8");

                    if (columnValueStr == null || !columnValueStr.equals(equalsValue)) {
                        // 如果列不存在或值不匹配，返回 FAIL
                        res.status(409,"Column name Not Found");  // 保持 200 状态码
                        return "FAIL";
                    }
                } else {
                    // 行不存在，返回 404
                    res.status(404, "Row not found");
                    return "FAIL";
                }
            }

            // 返回分配的版本号
            int latestVersion =putData(tableName, rowKey, columnKey, data);
            res.header("Version", String.valueOf(latestVersion));

            // 返回 OK 表示操作成功
            res.status(200,"successful");
            return "OK";
        });
        get("/data/:table/:row/:column",(req,res)->{
            String tableName = req.params("table");
            String rowKey = req.params("row");
            String columnKey = req.params("column");
            String versionParam = req.queryParams("version");
            String result;
            if (versionParam != null) {
                try {
                    int version = Integer.parseInt(versionParam);  // 解析版本号
                    result = getDataByVersion(tableName, rowKey, columnKey, version);  // 获取指定版本的数据
                    res.header("Version", String.valueOf(version));
                } catch (NumberFormatException e) {
                    res.status(400, "Invalid version number");
                    return "Invalid version parameter";
                }
            } else {
                result = getdata(tableName, rowKey, columnKey);
                Map<String, Row> table = dataStore.get(tableName);
                Row row = table.get(rowKey);
                int latestVersion = row.getLatestVersion(columnKey);
                res.header("Version", String.valueOf(latestVersion));
            }
            // 打印结果并返回响应
            System.out.println(result);
            if (result != null) {
                res.status(200, "successful");
                return result;
            } else {
                res.status(404, "not found");
                return "Data not found";
            }
        });
    }
    private static String generateRandomId() {
        Random random = new Random();
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char randomChar = (char) ('a' + random.nextInt(26));
            id.append(randomChar);
        }
        return id.toString();
    }
    private static void sendpingrequest(String workerId,String IP,String workerPort, int port) throws IOException {
        Socket socket=new Socket(IP, port);
        String pingRequest = "GET /ping?id=" + workerId + "&port=" + workerPort + " HTTP/1.1\r\n" +
                "Host: " + IP + "\r\n" +
                "Connection: close\r\n\r\n";
        OutputStream out=socket.getOutputStream();
        PrintWriter pw=new PrintWriter(out);
        pw.println(pingRequest);
        pw.flush();
    }
    private static synchronized String readfromFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            } else {
                String workerId = generateRandomId();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(workerId);
                }
                return workerId;
            }
        }
    }



    private static int putData(String tableName, String rowKey, String columnKey, byte[] data) {
        dataStore.putIfAbsent(tableName, new ConcurrentHashMap<>());//获取该表，若不存在就新建一个
        Map<String, Row> content=dataStore.get(tableName);//获取该表中的行
        content.putIfAbsent(rowKey, new Row(rowKey));//如果行不存在就创建一个新行
        Row row = content.get(rowKey);//获取该行
        row.put(columnKey, data);//输入数据
        return row.getLatestVersion(columnKey);
    }
    private static String getdata(String tableName, String rowKey, String columnKey) throws UnsupportedEncodingException {
        if(!dataStore.containsKey(tableName)){
            return null;
        }
        Map<String, Row> content=dataStore.get(tableName);
        if(!content.containsKey(rowKey)){
            return  null;
        }
        Row row = content.get(rowKey);
        byte[] dataBytes = row.get(columnKey);  // 假设 row.get 返回 byte[] 类型
        if (dataBytes == null) {
            return null;
        }

        // 将 byte[] 转换为 String
        String data = new String(dataBytes, "UTF-8");
        if(data == null){
            return null;
        }
        return data;
    }
    private static String getDataByVersion(String tableName, String rowKey, String columnKey, int version) throws UnsupportedEncodingException {
        if (!dataStore.containsKey(tableName)) {
            return null;
        }
        Map<String, Row> content = dataStore.get(tableName);
        if (!content.containsKey(rowKey)) {
            return null;
        }
        Row row = content.get(rowKey);

        // 假设 row.getVersion(columnKey, version) 返回的是指定版本的 byte[]
        byte[] dataBytes = row.getVersion(columnKey, version);  // 根据版本号获取数据
        if (dataBytes == null) {
            return null;
        }

        // 将 byte[] 转换为 String，假设使用 UTF-8 编码
        return new String(dataBytes, "UTF-8");
    }


}