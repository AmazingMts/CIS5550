package cis5550.webserver;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import cis5550.webserver.Route;
import cis5550.webserver.Server;

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
                StringBuilder headerBuilder = new StringBuilder();
                String line;
                int contentLength = 0;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    headerBuilder.append(line).append("\r\n");
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }
                headerBuilder.append("\r\n");

                if (line == null) {
                    break;
                }

                String headers = headerBuilder.toString();
                String[] lines = headers.split("\r\n");
                String[] firstLineParts = lines[0].split(" ");
                if (firstLineParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request", keepAlive);
                    break;
                }

                String method = firstLineParts[0];
                String url = firstLineParts[1];
                String httpVersion = firstLineParts[2];
                String requestFilepath = directory + url;
                File requestFile = new File(requestFilepath);
                //exam the httpversion
                if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
                    continue;
                }
                //examine the method
                Route route = null;
                switch (method) {
                    case "GET":
                        route = Server.getRoutes.get(url);
                        break;
                    case "POST":
                        route = Server.postRoutes.get(url);
                        break;
                    case "PUT":
                        route = Server.putRoutes.get(url);
                        break;
                    default:
                        sendErrorResponse(os, 501, "Not Implement", keepAlive);
                        continue;
                }
                if (route != null) {
                    try {
                        // 创建请求和响应对象
                        Request request = new RequestImpl(method, url, httpVersion, parseHeaders(headers), parseQueryParams(url), null, (InetSocketAddress) socket.getRemoteSocketAddress(), readRequestBody(is, contentLength), Server.getInstance());
                        Response response = new ResponseImpl();

                        Object result=route.handle(request, response);
                        response.body(result.toString());

                        sendResponse(socket.getOutputStream(), response);
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                // routes
//                    Route route = null;
//                    if (method.equals("GET")) {
//                        route = Server.getRoutes.get(url);
//                    } else if (method.equals("POST")) {
//                        route = Server.postRoutes.get(url);
//                    } else if (method.equals("PUT")) {
//                        route = Server.putRoutes.get(url);
//                    } else {
//                        sendErrorResponse(os, 501, "Not Implemented", keepAlive);
//                        continue;
//                    }
//                }
//                if (route != null) {
//                    route.handle(socket, directory);
//                } else {
//                    sendErrorResponse(os, 404, "Not Found", keepAlive);
//                }

//                    if(route!=null) {
//                        route.handle(socket,directory);
//                    }else {
//                        sendErrorResponse(os, 400, "Bad Request", keepAlive);
//                    }
//                }else if(!httpVersion.equals("HTTP/1.1")) {
//                    sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
//                }else{
//                    sendErrorResponse(os, 501, "Not Implemented", keepAlive);
//                }
//                if (headers.contains("Connection: close")) {
//                    keepAlive = false;
//                }


                if (method.equals("POST") || method.equals("PUT")) {
                    sendErrorResponse(os, 405, "Method Not Allowed", keepAlive);
                } else if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
                } else if (url.contains("..")) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else if (!method.equals("GET") && !method.equals("HEAD")) {
                    sendErrorResponse(os, 501, "Not Implemented", keepAlive);
                } else if (!requestFile.exists()) {
                    sendErrorResponse(os, 404, "File Not Found", keepAlive);
                } else if (!requestFile.canRead()) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else if (contentLength > 0) {
                    char[] body = new char[contentLength];
                    reader.read(body, 0, contentLength);
                }
                else {
                    // Serve static file
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile))) {
                        byte[] fileContent = bis.readAllBytes();
                        Response response = new ResponseImpl();
                        response.bodyAsBytes(fileContent);
                        response.type(judgeContentType(requestFile.getAbsolutePath()));
                        response.status(200, "OK");
                        sendResponse(os, response);
                    } catch (Exception e) {
                        sendErrorResponse(os, 500, "Internal Server Error", keepAlive);
                    }
                }
            }
//                else {
//                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile));
//                    BufferedOutputStream bos = new BufferedOutputStream(os);
//                    bw.write("HTTP/1.1 200 OK\r\n");
//                    bw.write("Content-Type: " + judgeContentType(requestFile.getAbsolutePath()) + "\r\n");
//                    bw.write("Server: TianshiServer\r\n");
//                    bw.write("Content-Length: " + requestFile.length() + "\r\n");
//                    bw.write("\r\n");
//                    bw.flush();
//
//                    byte[] fileTransfer = new byte[1024];
//                    int fileTransferNum;
//                    while ((fileTransferNum = bis.read(fileTransfer)) != -1) {
//                        bos.write(fileTransfer, 0, fileTransferNum);
//                    }
//                    bos.flush();
//                    bis.close();  // 关闭文件流
//                    if (headers.contains("Connection: close")) {
//                        keepAlive = false;
//                    }
//                }
//            }
            // 检查 Connection: close 头部，如果存在则关闭连接


            // 默认情况下保持连接（HTTP/1.1 默认是持久连接）

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

    private Map<String, String> parseHeaders(String headers) {
        Map<String, String> headersMap = new HashMap<>();
        String[] lines = headers.split("\r\n");
        for (int i = 1; i < lines.length - 1; i++) {
            String[] headerParts = lines[i].split(":");
            if (headerParts.length == 2) {
                headersMap.put(headerParts[0].trim().toLowerCase(), headerParts[1].trim());
            }
        }
        return headersMap;
    }

    private Map<String, String> parseQueryParams(String url) {
        Map<String, String> queryParams = new HashMap<>();
        int questionMarkIndex = url.indexOf("?");
        if (questionMarkIndex != -1) {
            String queryString = url.substring(questionMarkIndex + 1);
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return queryParams;
    }

    private byte[] readRequestBody(InputStream is, int contentLength) throws IOException {
        if (contentLength > 0) {
            byte[] body = new byte[contentLength];
            int bytesRead = 0;
            while (bytesRead < contentLength) {
                int result = is.read(body, bytesRead, contentLength - bytesRead);
                if (result == -1) {
                    throw new IOException("Premature end of stream");
                }
                bytesRead += result;
            }
            return body;
        }
        return new byte[0];
    }


    private void sendResponse(OutputStream os, Response response) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {
            if (response instanceof ResponseImpl) {
                ResponseImpl impl = (ResponseImpl) response;
                if (impl.halted) {
                    bw.write("HTTP/1.1 " + impl.haltStatusCode + " " + impl.haltReasonPhrase + "\r\n");
                    bw.write("Connection: close\r\n");
                    bw.write("\r\n");
                    bw.flush();
                    return;
                }
            }
            bw.write("HTTP/1.1 " + response.getStatusCode() + " " + response.getStatusMessage() + "\r\n");
            for (AbstractMap.SimpleEntry<String, String> header : response.getHeaders()) {
                bw.write(header.getKey() + ": " + header.getValue() + "\r\n");
            }
            bw.write("Content-Type: " + response.getContentType() + "\r\n");
            byte[] body = response.getBody();
            if (body != null) {
                bw.write("Content-Length: " + body.length + "\r\n");
            } else {
                bw.write("Content-Length: 0\r\n");
            }
            bw.write("\r\n");
            bw.flush();
            if (body != null) {
                os.write(body);
                os.flush();
            }
        }
    }



}