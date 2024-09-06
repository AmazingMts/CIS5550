package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

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

        ServerSocket ss = new ServerSocket(port);
        logger.info("Server started on port " + port);

        while (true) {
            Socket socket = ss.accept();
            logger.info("Accepted connection from " + socket.getInetAddress());

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            boolean keepAlive = true;  // 默认情况下保持连接

            try {
                do {
                    // Read the request headers
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuilder headerBuilder = new StringBuilder();
                    while (!(line = reader.readLine()).isEmpty()) {
                        headerBuilder.append(line).append("\r\n");
                        if (line.toLowerCase().startsWith("connection: close")) {
                            keepAlive = false; // 客户端明确要求关闭连接
                        }
                    }
                    headerBuilder.append("\r\n"); // End of headers

                    String headers = headerBuilder.toString();
                    String[] lines = headers.split("\r\n");
                    String[] firstLineParts = lines[0].split(" ");

                    if (firstLineParts.length != 3) {
                        sendErrorResponse(os, 400, "Bad Request", keepAlive);
                        if (!keepAlive) {
                            break;
                        }
                        continue;
                    }

                    String method = firstLineParts[0];
                    String uri = firstLineParts[1];
                    String httpVersion = firstLineParts[2];
                    String requestFilepath = directory + uri;
                    File requestFile = new File(requestFilepath);

                    // Check method and version
                    if (method.equals("POST") || method.equals("PUT")) {
                        sendErrorResponse(os, 405, "POST or PUT are not allowed", keepAlive);
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
                        // Send the actual HTTP response
                         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                             FileInputStream fis = new FileInputStream(requestFile);
                             BufferedInputStream bis = new BufferedInputStream(fis);
                             BufferedOutputStream bos = new BufferedOutputStream(os);
                            // 发送响应头
                            bw.write("HTTP/1.1 200 OK\r\n");
                            bw.write("Content-Type: " + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                            bw.write("Server: TianshiServer\r\n");
                            bw.write("Content-Length: " + requestFile.length() + "\r\n");
                            bw.write("\r\n"); // 响应头结束
                            bw.flush();

                            // 发送文件数据
                            byte[] fileTransfer = new byte[1024];
                            int fileTransfernum;
                            while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
                                bos.write(fileTransfer, 0, fileTransfernum);
                            }
                            bos.flush();
                    }

                    // Check if we should keep the connection alive
                    if (!keepAlive) {
                        socket.shutdownOutput(); // 关闭输出流
                        break;
                    }
                } while (keepAlive); // Continue reading requests if keep-alive is true
            } catch (IOException e) {
                logger.warn("I/O error: " + e.getMessage());
            } finally {
                try {
                    socket.close(); // Always close the socket
                } catch (IOException e) {
                    logger.warn("Failed to close socket: " + e.getMessage());
                }
            }
        }


    }

    private static void sendErrorResponse(OutputStream os, int statusCode, String message, boolean keepAlive) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
        bw.write("Content-Type: text/plain\r\n");
        bw.write("Content-Length: " + message.length() + "\r\n");
        if (keepAlive) {
            bw.write("Connection: keep-alive\r\n");
        } else {
            bw.write("Connection: close\r\n");
        }
        bw.write("\r\n"); // End of headers
        bw.write(message);
        bw.flush();
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
