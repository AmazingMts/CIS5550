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
            boolean keepAlive = true;

            while (keepAlive) {
                // Read headers
                String line;
                StringBuilder headerBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    headerBuilder.append(line).append("\r\n");
                }
                headerBuilder.append("\r\n");

                // Check if the client has closed the connection
                if (line == null) {
                    // Client has closed the connection
                    break;
                }

                String headers = headerBuilder.toString();
                String[] lines = headers.split("\r\n");
                String[] firstLineParts = lines[0].split(" ");
                if (firstLineParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request", keepAlive);
                    break; // Invalid request, close the connection
                }

                String method = firstLineParts[0];
                String uri = firstLineParts[1];
                String httpVersion = firstLineParts[2];
                String requestFilepath = directory + uri;
                File requestFile = new File(requestFilepath);

                // Check for HTTP method and version
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
                    // Send file content
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile));
                    BufferedOutputStream bos = new BufferedOutputStream(os);

                    bw.write("HTTP/1.1 200 OK\r\n");
                    bw.write("Content-Type: " + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
                    bw.write("Server: TianshiServer\r\n");
                    bw.write("Content-Length: " + requestFile.length() + "\r\n");
                    bw.write("\r\n");
                    bw.flush();

                    byte[] fileTransfer = new byte[1024];
                    int fileTransfernum;
                    while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
                        bos.write(fileTransfer, 0, fileTransfernum);
                    }
                    bos.flush();
                }

                // Check for connection header to determine if we should keep the connection alive
                keepAlive = headers.contains("Connection: keep-alive");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the socket only after detecting client disconnection
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