package cis5550.kvs;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import cis5550.webserver.Server;

import static cis5550.webserver.Server.*;

public class Worker {
    private static Map<String, Map<String, Row>> dataStore = new HashMap<>();
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
                    String columnValue = row.get(ifcolumn);
                    if (columnValue == null || !columnValue.equals(equalsValue)) {
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

            putData(tableName, rowKey, columnKey, data);

            // 返回 OK 表示操作成功
            res.status(200,"successful");
            return "OK";
        });
        get("/data/:table/:row/:column",(req,res)->{
            String tableName = req.params("table");
            String rowKey = req.params("row");
            String columnKey = req.params("column");
            String result=getdata(tableName,rowKey,columnKey);
            System.out.println(result);
            if(result != null){
                res.status(200,"successful");
                return result;
            }
            else{
                res.status(404,"not found");
            }
            return null;
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
    private static String readfromFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        Scanner scanner = new Scanner(file);
        String workerId;
        if(scanner.hasNextLine()) {
            workerId = scanner.nextLine();
            return workerId;
        }
        else {
            workerId = generateRandomId();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(workerId);
            }
        }
        scanner.close();
        return workerId;
    }


    private static void putData(String tableName, String rowKey, String columnKey, byte[] data) {
        dataStore.putIfAbsent(tableName, new HashMap<>());//获取该表，若不存在就新建一个
        Map<String, Row> content=dataStore.get(tableName);//获取该表中的行
        content.putIfAbsent(rowKey, new Row(rowKey));//如果行不存在就创建一个新行
        Row row = content.get(rowKey);//获取该行
        row.put(columnKey, data);//输入数据
    }
    private static String getdata(String tableName, String rowKey, String columnKey) {
        if(!dataStore.containsKey(tableName)){
            return null;
        }
        Map<String, Row> content=dataStore.get(tableName);
        if(!content.containsKey(rowKey)){
            return  null;
        }
        Row row = content.get(rowKey);
        String data=row.get(columnKey);
        if(data == null){
            return null;
        }
        return data;
    }

}