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
    private static ExecutorService threadPool;

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
        threadPool = Executors.newFixedThreadPool(10);
        logger.info("Server started on port " + port);

        while (true) {
            Socket socket = ss.accept();
            threadPool.submit(() -> handleClient(socket, directory));
        }
    }

    private static void handleClient(Socket socket, String directory) {
        try {
            logger.info("Accepted connection from " + socket.getInetAddress());

            socket.setSoTimeout(500);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            boolean keepAlive = false;
            String connectionHeader = null;

            while (true) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int b;

                while ((b = is.read(bytes)) != -1) {
                    baos.write(bytes, 0, b);
                    byte[] data = baos.toByteArray();
                    if (data.length >= 4 &&
                            data[data.length - 4] == 13 && // CR
                            data[data.length - 3] == 10 && // LF
                            data[data.length - 2] == 13 && // CR
                            data[data.length - 1] == 10) { // LF
                        break;
                    }
                }

                String head = new String(baos.toByteArray(), "UTF-8");
                BufferedReader br = new BufferedReader(new StringReader(head));

                String firstLine = br.readLine();
                if (firstLine == null || firstLine.isEmpty()) {
                    sendErrorResponse(os, 400, "Bad Request");
                    return;
                }

                String[] firstLineParts = firstLine.split(" ");
                if (firstLineParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request");
                    return;
                }

                String method = firstLineParts[0];
                String uri = firstLineParts[1];
                String httpVersion = firstLineParts[2];

                String requestFilepath = directory + uri;
                File requestFile = new File(requestFilepath);

                if (method.equals("POST") || method.equals("PUT")) {
                    sendErrorResponse(os, 405, "POST or PUT are not allowed");
                    return;
                }

                if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported");
                    return;
                }

                if (uri.contains("..")) {
                    sendErrorResponse(os, 403, "Forbidden");
                    return;
                }

                if (!method.equals("GET") && !method.equals("HEAD")) {
                    sendErrorResponse(os, 501, "Not Implemented");
                    return;
                }

                if (!requestFile.exists()) {
                    sendErrorResponse(os, 404, "File Not Found");
                    return;
                }

                if (!requestFile.canRead()) {
                    sendErrorResponse(os, 403, "Forbidden");
                    return;
                }

                Map<String, String> headers = new HashMap<>();
                String headerLine;
                while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
                    logger.info("Header: " + headerLine);
                    if (headerLine.toLowerCase().startsWith("connection:")) {
                        connectionHeader = headerLine.split(":", 2)[1].trim();
                    }
                    String[] headerParts = headerLine.split(":", 2);
                    if (headerParts.length == 2) {
                        String name = headerParts[0].trim().toLowerCase();
                        String value = headerParts[1].trim();
                        headers.put(name, value);
                    }
                }

                // Check for keep-alive connection
                if ("keep-alive".equalsIgnoreCase(connectionHeader)) {
                    keepAlive = true;
                }

                int contentLength = 0;
                if (headers.containsKey("content-length")) {
                    try {
                        contentLength = Integer.parseInt(headers.get("content-length"));
                    } catch (NumberFormatException e) {
                        sendErrorResponse(os, 400, "Bad Request");
                        return;
                    }
                }

                if (contentLength > 0) {
                    byte[] content = new byte[contentLength];
                    int bytesRead = is.read(content, 0, contentLength);
                    if (bytesRead != contentLength) {
                        sendErrorResponse(os, 400, "Bad Request");
                        return;
                    }
                    System.out.println("Message Body (bytes): " + new String(content));
                }

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write("HTTP/1.1 200 OK\r\n");
                bw.write("Content-Type:" + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                bw.write("Server: TianshiServer\r\n");
                bw.write("Content-Length:" + requestFile.length() + "\r\n");
                bw.write("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");
                bw.write("\r\n");
                bw.flush();

                try (FileInputStream fis = new FileInputStream(requestFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    byte[] fileTransfer = new byte[1024];
                    int fileTransfernum;
                    while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
                        try {
                            os.write(fileTransfer, 0, fileTransfernum);
                        } catch (SocketException se) {
                            logger.error("Client disconnected while sending file", se);
                            break;
                        }
                    }
                    os.flush();
                } catch (IOException e) {
                    logger.error("Error sending file", e);
                }

                if (!keepAlive) {
                    break; // Exit loop if the connection is not to be kept alive
                }
            }

        } catch (IOException e) {
            logger.error("Error handling client request", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }
    }

    private static void sendErrorResponse(OutputStream os, int statusCode, String statusMessage) throws IOException {
        PrintWriter pw = new PrintWriter(os);
        pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        pw.println("Content-Type: text/plain");
        pw.println("Content-Length: " + statusMessage.length());
        pw.println("Connection: close");
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
