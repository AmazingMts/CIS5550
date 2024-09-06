package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cis5550.tools.Logger;
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            logger.error("Invalid amount of parameters. Expected two, but got " + args.length);
            System.out.println("Written by Tianshi Miao");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String directory = args[1];
        File directoryFile = new File(directory);
        if (!directoryFile.exists() || !directoryFile.isDirectory()) {
            logger.error("Directory path " + directory + " is not valid");
            return;
        }

        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Server started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            logger.info("Accepted connection from " + socket.getInetAddress());

            new Thread(() -> handleClient(socket, directory)).start();
        }
    }

    private static void handleClient(Socket socket, String directory) {
        try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
            boolean keepAlive = true; // 默认开启持久连接

            while (keepAlive) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String firstLine = br.readLine();

                if (firstLine == null || firstLine.isEmpty()) {
                    sendErrorResponse(os, 400, "Bad Request");
                    return;
                }

                String[] requestParts = firstLine.split(" ");
                if (requestParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request");
                    return;
                }

                String method = requestParts[0];
                String uri = requestParts[1];
                String httpVersion = requestParts[2];

                if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported");
                    return;
                }

                // 读取请求头
                String headerLine;
                int contentLength = 0;
                boolean connectionClose = false; // 标记是否为 Connection: close

                while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
                    logger.info("Header: " + headerLine);
                    if (headerLine.toLowerCase().startsWith("content-length")) {
                        contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                    } else if (headerLine.toLowerCase().startsWith("connection")) {
                        String connectionValue = headerLine.split(":")[1].trim().toLowerCase();
                        if (connectionValue.equals("close")) {
                            connectionClose = true;
                        }
                    }
                }

                // 判断是否要关闭连接
                if (connectionClose) {
                    keepAlive = false;
                }

                File requestFile = new File(directory + uri);
                if (!requestFile.exists()) {
                    sendErrorResponse(os, 404, "File Not Found");
                    return;
                }

                if (!requestFile.canRead()) {
                    sendErrorResponse(os, 403, "Forbidden");
                    return;
                }

                // 发送响应
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write("HTTP/1.1 200 OK\r\n");
                bw.write("Content-Type:" + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                bw.write("Content-Length:" + requestFile.length() + "\r\n");
                bw.write("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");
                bw.write("\r\n");
                bw.flush();

                // 发送文件内容
                try (FileInputStream fis = new FileInputStream(requestFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }

                if (!keepAlive) {
                    logger.info("Closing connection as requested by client.");
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error handling client connection: " + e.getMessage());
        }
    }

    private static void sendErrorResponse(OutputStream os, int statusCode, String statusMessage) throws IOException {
        PrintWriter pw = new PrintWriter(os);
        pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        pw.println("Content-Type: text/plain");
        pw.println("Content-Length: " + statusMessage.length());
        pw.println();
        pw.println(statusMessage);
        pw.flush();
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

