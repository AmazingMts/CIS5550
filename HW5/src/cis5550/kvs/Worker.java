package cis5550.kvs;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import cis5550.webserver.Server;
import cis5550.tools.*;
import static cis5550.webserver.Server.*;
import cis5550.kvs.*;

public class Worker {
    private static Map<String, Map<String, Row>> dataStore = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            return;
        }
        String PortNumber=args[0];
        String directory=args[1];
        String[] coordnet=args[2].split(":");
        String IP=coordnet[0];
        int Port=Integer.parseInt(coordnet[1]);
        port(Integer.parseInt(PortNumber));
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
            if (tableName.startsWith("pt-")) {
                // 1. 检查或创建表目录
                File tableDirectory = new File(directory, tableName);
                if (!tableDirectory.exists()) {
                    tableDirectory.mkdir();  // 创建存储表数据的目录
                }

                // 2. 对行键进行编码
                String encodedKey = KeyEncoder.encode(rowKey);
                File rowFile = new File(tableDirectory, encodedKey);
                Row row = null;

                // 3. 检查文件是否存在
                if (rowFile.exists()) {
                    // 文件存在，读取现有数据并反序列化
                    byte[] existingData = Files.readAllBytes(rowFile.toPath());
                    row = Row.fromByteArray(existingData);  // 反序列化为 Row 对象
                } else {
                    // 文件不存在，创建新的 Row 对象
                    row = new Row(rowKey);
                }
                // 4. 更新或添加列数据
                row.put(columnKey, data);  // 将请求体中的数据更新到指定列
                // 5. 将更新后的行数据写回磁盘
                try (FileOutputStream fos = new FileOutputStream(rowFile)) {
                    fos.write(row.toByteArray());  // 序列化行数据并写入文件（覆盖旧内容）
                } catch (IOException e) {
                    e.printStackTrace();
                    res.status(500, "Error writing row file.");
                }
                // 6. 返回成功响应
                res.status(200, "OK");
                res.body("OK");
            }
            else {
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
                            res.status(200, "Column name Not Found");  // 保持 200 状态码
                            return "FAIL";
                        }
                    } else {
                        // 行不存在，返回 404
                        res.status(404, "Row not found");
                        return "FAIL";
                    }
                }

                // 返回分配的版本号

                int latestVersion = putData(tableName, rowKey, columnKey, data);
                res.header("Version", String.valueOf(latestVersion));

                // 返回 OK 表示操作成功
                res.status(200, "successful");
                return "OK";
            }
            return null;
        });

        get("/data/:table/:row", (req, res) -> {
            String tableName = req.params("table");
            String rowKey = req.params("row");

            // 获取行数据
            Row row = getRow(directory, tableName, rowKey);
            if (row != null) {
                // 将行序列化为字节数组并返回
                byte[] rowData = row.toByteArray();
                // 将字节数组转换为可读的字符串
                String readableData = new String(rowData, StandardCharsets.UTF_8);

                // 返回可读的行数据
                res.body(readableData);
//                System.out.println(readableData);
                res.status(200, "OK");
                return readableData;
            } else {
                res.status(404, "Row not found");
                return null;
            }
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
                } catch (NumberFormatException e) {
                    res.status(400, "Invalid version number");
                    return "Invalid version parameter";
                }
            } else {
                result = getdata(directory,tableName, rowKey, columnKey);  // 获取最新版本的数据
            }
            // 打印结果并返回响应
            if (result != null) {
                res.status(200, "successful");
                return result;
            } else {
                res.status(404, "not found");
                return "Data not found";
            }
        });
        get("/data/:table", (req, res) -> {
            String tableName = req.params("table");
            String startRow = req.queryParams("startRow");
            String endRowExclusive = req.queryParams("endRowExclusive");

            // 检查是否是持久表（pt-开头的表名为持久表）
            boolean isPersistentTable = tableName.startsWith("pt-");
            StringBuilder responseBuilder = new StringBuilder();
            boolean tableFound = false;

            // 如果是持久表，从磁盘读取
            if (isPersistentTable) {
                File tableDirectory = new File(directory, tableName);
                if (tableDirectory.exists() && tableDirectory.isDirectory()) {
                    File[] rowFiles = tableDirectory.listFiles();
                    if (rowFiles != null) {
                        for (File rowFile : rowFiles) {
                            String rowKey = KeyEncoder.decode(rowFile.getName());

                            // 根据startRow和endRowExclusive进行筛选
                            if (startRow != null && rowKey.compareTo(startRow) < 0) continue;
                            if (endRowExclusive != null && rowKey.compareTo(endRowExclusive) >= 0) continue;

                            // 读取并序列化行数据
                            byte[] rowData = Files.readAllBytes(rowFile.toPath());
                            Row row = Row.fromByteArray(rowData);
                            responseBuilder.append(new String(row.toByteArray(), "UTF-8")).append("\n");
                        }
                        tableFound = true;
                    }
                }
            } else {
                // 如果不是持久表，从内存读取
                if (dataStore.containsKey(tableName)) {
                    Map<String, Row> table = dataStore.get(tableName);
                    for (String rowKey : table.keySet()) {
                        // 根据startRow和endRowExclusive进行筛选
                        if (startRow != null && rowKey.compareTo(startRow) < 0) continue;
                        if (endRowExclusive != null && rowKey.compareTo(endRowExclusive) >= 0) continue;

                        // 获取行数据并序列化
                        Row row = table.get(rowKey);
                        responseBuilder.append(new String(row.toByteArray(), "UTF-8")).append("\n");
                    }
                    tableFound = true;
                }
            }

            // 如果找到了表，返回数据
            if (tableFound) {
                if (responseBuilder.length() == 0) {
                    res.status(404, "No matching rows found");
                    return "No matching rows found";
                }
                res.status(200, "OK");
                res.type("text/plain");
                res.body(responseBuilder.toString() + "\n");
                return null;
            }

            // 如果未找到表，返回404
            res.status(404, "Table not found");
            return "Table not found";
        });


        get("/tables", (req, res) -> {
            StringBuilder responseText = new StringBuilder();

            // 遍历内存中的表并添加到响应
            dataStore.keySet().forEach(tableName -> {
                responseText.append(tableName).append("\n");
            });

            // 遍历持久化表并添加到响应
            File storageDir = new File(directory);
            if (storageDir.exists() && storageDir.isDirectory()) {
                for (File tableDirectory : storageDir.listFiles()) {
                    if (tableDirectory.isDirectory()) {
                        responseText.append(tableDirectory.getName()).append("\n");
                    }
                }
            }

            // 设置响应头为 text/plain
            res.type("text/plain");

            // 返回表名列表
            return responseText.toString();
        });

        get("/", (req, res) -> {
            StringBuilder html = new StringBuilder("<html><body><table>");

            // 遍历内存中的表并生成 HTML 表格内容
            dataStore.keySet().forEach(tableName -> {
                html.append("<tr><td>").append(tableName).append("</td></tr>");
            });

            // 遍历持久化表并生成 HTML 表格内容
            File storageDir = new File(directory);
            if (storageDir.exists() && storageDir.isDirectory()) {
                for (File tableDirectory : storageDir.listFiles()) {
                    if (tableDirectory.isDirectory()) {
                        html.append("<tr><td>").append(tableDirectory.getName()).append("</td></tr>");
                    }
                }
            }

            html.append("</table></body></html>");

            // 设置响应头为 text/html
            res.type("text/html");

            // 返回生成的 HTML 页面
            return html.toString();
        });
        get("/count/:table",(req,res)->{
            String tableName = req.params("table");
            File tableDirectory = new File(directory, tableName);
            if (dataStore.containsKey(tableName)) {
                int rowCount = dataStore.get(tableName).size();  // 获取内存表中的行数
                res.status(200, "OK");
                res.type("text/plain");
                return String.valueOf(rowCount);  // 返回行数作为响应
            }
            if (tableDirectory.exists() && tableDirectory.isDirectory()) {
                // 持久化表，读取表目录中的文件数作为行数
                String[] rowFiles = tableDirectory.list();  // 获取该表的行文件
                if (rowFiles != null) {
                    int rowCount = rowFiles.length;
                    res.status(200, "OK");
                    res.type("text/plain");
                    return String.valueOf(rowCount);  // 返回行数作为响应
                }
            }

            // 如果表不存在，返回 404
            res.status(404, "Table not found");
            return "Table not found";
        });


        put("/rename/:table", (req, res) -> {
            String tableName = req.params("table");  // 当前表名 XXX
            String newTableName = req.body();  // 新的表名 YYY 从请求体中获取

            // 检查是否提供了新的表名
            if (newTableName == null || newTableName.trim().isEmpty()) {
                res.status(400, "New table name not provided");
                return "New table name not provided";
            }

            // 检查内存中的表
            boolean inMemory = dataStore.containsKey(tableName);

            // 检查磁盘上的表
            File tableDirectory = new File(directory, tableName);
            boolean onDisk = tableDirectory.exists() && tableDirectory.isDirectory();

            // 如果内存和磁盘上都不存在表，则返回404错误
            if (!inMemory && !onDisk) {
                res.status(404, "Table not found");
                return "Table not found";
            }

            // 检查新表名是否已存在于磁盘
            File newTableDirectory = new File(directory, newTableName);
            if (newTableDirectory.exists()) {
                res.status(409, "Target table already exists");
                return "Target table already exists";
            }

            // 检查命名规则：如果 XXX 是持久化表 (pt- 开头)，但 YYY 不是持久化表，则返回 400
            if (tableName.startsWith("pt-") && !newTableName.startsWith("pt-")) {
                res.status(400, "Invalid target table name for persistent table");
                return "Persistent table names must start with 'pt-'";
            }

            // 先重命名内存中的表
            if (inMemory) {
                Map<String, Row> rows = dataStore.remove(tableName);  // 从旧表名中移除
                dataStore.put(newTableName, rows);  // 使用新表名插入
            }

            // 然后重命名磁盘上的表（如果存在）
            if (onDisk) {
                boolean success = tableDirectory.renameTo(newTableDirectory);
                if (!success) {
                    res.status(500, "Failed to rename table on disk");
                    return "Failed to rename table on disk";
                }
            }

            // 返回成功响应
            res.status(200, "OK");
            return "OK";
        });
        get("/check/:table", (req, res) -> {
            String tableName = req.params("table");  // 获取表名

            // 检查内存中的表是否存在
            if (dataStore.containsKey(tableName)) {
                res.status(200, "Table exists in memory");
                return "Table " + tableName + " exists in memory.";
            } else {
                res.status(404, "Table not found in memory");
                return "Table " + tableName + " not found in memory.";
            }
        });



        put("/delete/:table", (req, res) -> {
            String tableName = req.params("table");  // 获取表名 XXX


            // 检查表目录是否存在（用于持久化表）
            File tableDirectory = new File(directory, tableName);


            // 检查内存中的表是否存在（如果有内存存储结构）
            boolean inMemory = dataStore.containsKey(tableName);

            // 检查表是否存在（内存表或持久化表）
            if (!inMemory && (!tableDirectory.exists() || !tableDirectory.isDirectory())) {
                res.status(404, "Table not found");
                return "Table not found";
            }

            // 删除内存中的表（如果存在内存表）
            if (inMemory) {
                dataStore.remove(tableName);  // 从内存中删除表
            }

            // 删除持久化表目录及其文件（如果是持久化表）
            if (tableDirectory.exists() && tableDirectory.isDirectory()) {
                deleteDirectory(tableDirectory);  // 删除磁盘上的表目录及所有文件
            }

            // 返回成功响应
            res.status(200, "OK");
            return "OK";
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
    private static String getdata(String real_directory,String tableName, String rowKey, String columnKey) throws Exception {
        String directory = real_directory;
        File tableDirectory = new File(directory, tableName);
        if (tableDirectory.exists() && tableDirectory.isDirectory()) {
            // 如果表的文件夹存在，说明是持久化表，从磁盘读取数据
            String encodedKey = KeyEncoder.encode(rowKey);  // 对行键进行编码
            File rowFile = new File(tableDirectory, encodedKey);
            if (!rowFile.exists()) {
                return null;  // 如果行文件不存在，返回 null
            }

            // 从磁盘读取行数据
            byte[] existingData = Files.readAllBytes(rowFile.toPath());
            Row row = Row.fromByteArray(existingData);  // 反序列化为 Row 对象

            // 获取列数据
            byte[] dataBytes = row.get(columnKey);
            if (dataBytes == null) {
                return null;
            }

            return new String(dataBytes, "UTF-8");
        }
        else {
            if (!dataStore.containsKey(tableName)) {
                System.out.println("到判断dataStore为空了");
                return null;
            }
            Map<String, Row> content = dataStore.get(tableName);
            if (!content.containsKey(rowKey)) {
                return null;
            }
            Row row = content.get(rowKey);
            byte[] dataBytes = row.get(columnKey);  // 假设 row.get 返回 byte[] 类型
            if (dataBytes == null) {
                return null;
            }

            // 将 byte[] 转换为 String
            String data = new String(dataBytes, "UTF-8");
            if (data == null) {
                return null;
            }
            return data;
        }
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
    private static void deleteDirectory(File directory) {
        // 递归删除目录中的所有文件和子目录
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);  // 递归删除子目录
                } else {
                    file.delete();  // 删除文件
                }
            }
        }
        // 删除空目录
        directory.delete();
    }
    private static Map<String, Row> getTable(String directory, String tableName) {
        if (dataStore.containsKey(tableName)) {
            Map<String, Row> table = dataStore.get(tableName);
            return table;  // 返回表结构
        }
        return null;  // 如果表不存在，返回 null
    }

    private static Row getRow(String directory, String tableName, String rowKey) throws Exception {
        // 首先检查是否在内存中
        if (dataStore.containsKey(tableName)) {
            Map<String, Row> table = dataStore.get(tableName);
            if (table.containsKey(rowKey)) {
                return table.get(rowKey);  // 返回内存中的 Row 对象
            }
        }


        File tableDirectory = new File(directory, tableName);
        if (!tableDirectory.exists() || !tableDirectory.isDirectory()) {
            return null; // 如果表目录不存在，返回 null
        }

        // 编码行键
        String encodedKey = KeyEncoder.encode(rowKey);
        File rowFile = new File(tableDirectory, encodedKey);
        if (!rowFile.exists()) {
            return null; // 如果行文件不存在，返回 null
        }

        // 从磁盘读取行数据
        byte[] rowData = Files.readAllBytes(rowFile.toPath());

        // 反序列化为 Row 对象
        return Row.fromByteArray(rowData);
    }

}