package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cis5550.tools.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        // 检查参数数量
        if (args.length != 2) {
            logger.error("Invalid amount of parameters. Expected two, but got " + args.length);
            System.out.println("Written by Tianshi Miao");
            return;
        }
        // 验证端口号和目录
        int port = Integer.parseInt(args[0]);
        String directory = args[1];
        File directoryFile = new File(directory);
        if (!directoryFile.exists() || !directoryFile.isDirectory()) {
            logger.error("Directory path " + directory + " is not valid");
            return;
        }

        // 创建ServerSocket
        ServerSocket ss = new ServerSocket(port);
        logger.info("Server started on port " + port);

        // 监听客户端连接
        while (true) {
            Socket socket = null;
            try {
                // Accept client connection
                socket = ss.accept();
                logger.info("Accepted connection from " + socket.getInetAddress());

                // 提交到线程池
                Socket finalSocket = socket;
                executor.submit(() -> handleRequest(finalSocket, directory));
            } catch (IOException e) {
                logger.error("Error accepting connection: " + e.getMessage());
            }
        }
    }

    private static void handleRequest(Socket socket, String directory) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream os = socket.getOutputStream()) {

            String line;
            StringBuilder requestHeaders = new StringBuilder();

            // 逐行读取请求头
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                requestHeaders.append(line).append("\r\n");
            }
            requestHeaders.append("\r\n"); // Add end of headers

            String request = requestHeaders.toString();
            logger.info("Received request: \n" + request);

            // 解析请求行
            String[] lines = request.split("\r\n");
            if (lines.length > 0) {
                String[] firstLineParts = lines[0].split(" ");
                if (firstLineParts.length == 3) {
                    String method = firstLineParts[0];
                    String uri = firstLineParts[1];
                    String httpVersion = firstLineParts[2];
                    String requestFilepath = directory + uri;
                    File requestFile = new File(requestFilepath);

                    // Check method and version
                    if (method.equals("POST") || method.equals("PUT")) {
                        sendErrorResponse(os, 405, "POST or PUT are not allowed");
                    } else if (!httpVersion.equals("HTTP/1.1")) {
                        sendErrorResponse(os, 505, "HTTP Version Not Supported");
                    } else if (uri.contains("..")) {
                        sendErrorResponse(os, 403, "Forbidden");
                    } else if (!method.equals("GET") && !method.equals("HEAD")) {
                        sendErrorResponse(os, 501, "Not Implemented");
                    } else if (!requestFile.exists()) {
                        sendErrorResponse(os, 404, "File Not Found");
                    } else if (!requestFile.canRead()) {
                        sendErrorResponse(os, 403, "Forbidden");
                    } else {
                        // Send the actual HTTP response
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                        bw.write("HTTP/1.1 200 OK\r\n");
                        bw.write("Content-Type: " + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                        bw.write("Server: TianshiServer\r\n");
                        bw.write("Content-Length: " + requestFile.length() + "\r\n");
                        bw.write("\r\n"); // End of headers
                        bw.flush();

                        FileInputStream fis = new FileInputStream(requestFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        byte[] fileTransfer = new byte[1024];
                        int fileTransfernum;
                        while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
                            os.write(fileTransfer, 0, fileTransfernum);
                        }
                        os.flush();
                    }
                } else {
                    sendErrorResponse(os, 400, "Bad Request");
                }
            } else {
                sendErrorResponse(os, 400, "Bad Request");
            }
        } catch (IOException e) {
            logger.error("Error handling request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket: " + e.getMessage());
            }
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
