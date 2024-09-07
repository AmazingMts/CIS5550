package cis5550.webserver;

import java.io.*;
import java.net.Socket;

public class YourRunnable implements Runnable {
    Socket socket;
    String directory;

    public YourRunnable(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            boolean keepAlive = true; // 默认情况下保持连接

            while (keepAlive) {
                // 读取请求头
                StringBuilder headerBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    headerBuilder.append(line).append("\r\n");
                }
                headerBuilder.append("\r\n");

                // 检查客户端是否关闭了连接
                if (line == null) {
                    // 客户端已关闭连接
                    break;
                }

                String headers = headerBuilder.toString();
                String[] lines = headers.split("\r\n");
                String[] firstLineParts = lines[0].split(" ");
                if (firstLineParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request", keepAlive);
                    break; // 请求无效，关闭连接
                }

                String method = firstLineParts[0];
                String uri = firstLineParts[1];
                String httpVersion = firstLineParts[2];
                String requestFilepath = directory + uri;
                File requestFile = new File(requestFilepath);

                // 检查 HTTP 方法和版本
                if (method.equals("POST") || method.equals("PUT")) {
                    sendErrorResponse(os, 405, "Method Not Allowed", keepAlive);
                } else if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
                } else if (uri.contains("..")) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else if (!method.equals("GET") && !method.equals("HEAD")) {
                    sendErrorResponse(os, 501, "Not Implemented", keepAlive);
                } else if (!requestFile.exists()) {
                    sendErrorResponse(os, 404, "File Not Found", keepAlive);
                } else if (!requestFile.canRead()) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else {
                    // 发送文件内容
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile));
                    BufferedOutputStream bos = new BufferedOutputStream(os);

                    bw.write("HTTP/1.1 200 OK\r\n");
                    bw.write("Content-Type: " + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                    bw.write("Server: TianshiServer\r\n");
                    bw.write("Content-Length: " + requestFile.length() + "\r\n");
                    bw.write("\r\n");
                    bw.flush();

                    byte[] fileTransfer = new byte[1024];
                    int fileTransferNum;
                    while ((fileTransferNum = bis.read(fileTransfer)) != -1) {
                        bos.write(fileTransfer, 0, fileTransferNum);
                    }
                    bos.flush();
                    bis.close();  // 关闭文件流
                }

                // 检查 Connection: close 头部，如果存在则关闭连接
                if (headers.contains("Connection: close")) {
                    keepAlive = false;
                }

                // 默认情况下保持连接（HTTP/1.1 默认是持久连接）
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 确保在客户端断开后关闭套接字
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void sendErrorResponse(OutputStream os, int statusCode, String message, boolean keepAlive) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {
            bw.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
            bw.write("Content-Type: text/plain\r\n");
            bw.write("Content-Length: " + message.length() + "\r\n");
            if (keepAlive) {
                bw.write("Connection: keep-alive\r\n");
            } else {
                bw.write("Connection: close\r\n");
            }
            bw.write("\r\n");
            bw.write(message);
            bw.flush();
        }
    }

    private static String judgeContentType(String filepath) {
        if (filepath.endsWith(".jpg") || filepath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filepath.endsWith(".txt")) {
            return "text/plain";
        } else if (filepath.endsWith(".html")) {
            return "text/html";
        } else {
            return "application/octet-stream";
        }
    }
}