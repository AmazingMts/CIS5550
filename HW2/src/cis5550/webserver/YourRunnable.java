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
            boolean keepAlive = true;


            while (keepAlive) {
                StringBuilder headerBuilder = new StringBuilder();
                Map<String, String> headersMap = new HashMap<>();
                String line;
                int contentLength = 0;
                String firstLine = null;
                if ((firstLine = reader.readLine()) != null && !firstLine.isEmpty()) {
                    headerBuilder.append(firstLine).append("\r\n");
                }
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    headerBuilder.append(line).append("\r\n");

                    // 解析请求头并存储在 headersMap 中
                    String[] headerParts = line.split(":");
                    if (headerParts.length == 2) {
                        String key = headerParts[0].trim();
                        String value = headerParts[1].trim();
                        headersMap.put(key, value); // 存储请求头
                    }

                    // 检查 Content-Length
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }
                headerBuilder.append("\r\n");

                String headers = headerBuilder.toString();

                String[] firstLineParts = firstLine.split(" ");
                if (firstLineParts.length != 3) {
                    sendErrorResponse(os, 400, "Bad Request", keepAlive);
                    break;
                }


//                byte[] bodyBytes = new byte[contentLength];
//                int bytesRead = 0;
//                int totalBytesRead = 0;
//
//                while (totalBytesRead < contentLength) {
//                    bytesRead = is.read(bodyBytes, totalBytesRead, contentLength - totalBytesRead);
//                    totalBytesRead += bytesRead;
//                }
                StringBuilder bodyBuilder = new StringBuilder();
                if (contentLength > 0) {
                    char[] bodyBuffer = new char[contentLength];
                    int bytesRead = reader.read(bodyBuffer, 0, contentLength);
                    if (bytesRead != contentLength) {
                        sendErrorResponse(os, 400, "Bad Request", keepAlive);
                        break;
                    }
                    bodyBuilder.append(bodyBuffer, 0, bytesRead);
                }

                String requestBody = bodyBuilder.toString();
                byte[] bodyBytes = requestBody.getBytes();

                String method = firstLineParts[0];
                String url = firstLineParts[1];
                String httpVersion = firstLineParts[2];
                String requestFilepath = directory + url;
                File requestFile = new File(requestFilepath);
                System.out.println(socket.getLocalPort());
                boolean isHttps= socket.getLocalPort() == 8443||socket.getLocalPort() == 443;
                // Check HTTP version
                if (!httpVersion.equals("HTTP/1.1")) {
                    sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
                    continue;
                }
//                if (contentLength > 0) {
//                    body = new byte[contentLength];
//                    int bytesRead = 0;
//                    while (bytesRead < contentLength) {
//                        int result = is.read(body, bytesRead, contentLength - bytesRead);
//                        if (result == -1) {
//                            throw new IOException("Premature end of stream");
//                        }
//                        bytesRead += result;
//                    }
//                }

                // Handle dynamic routes
                Map<String, String> queryParams = parseQueryParams(url);

                Route route = null;
                Map<String, String> params = null;
                switch (method) {
                    case "GET":
                        route = Server.getRoutes.get(url.split("\\?")[0]);
                        break;
                    case "POST":
                        route = Server.postRoutes.get(url.split("\\?")[0]);
                        break;
                    case "PUT":
                        route = Server.putRoutes.get(url.split("\\?")[0]);
                        break;
                    default:
                        sendErrorResponse(os, 501, "Not Implemented", keepAlive);
                        continue;
                }

                if (route == null) {
                    //match para
                    for (Map.Entry<String, Route> entry : Server.getRoutes.entrySet()) {
                        params = matchPath(entry.getKey(), url);
                        if (params != null) {
                            route = entry.getValue();
                            break;
                        }
                    }
                    if (route == null) {
                        for (Map.Entry<String, Route> entry : Server.putRoutes.entrySet()) {
                            params = matchPath(entry.getKey(), url);
                            if (params != null) {
                                route = entry.getValue();
                                break;
                            }
                        }
                    }
                }

                if (route != null) {
                    try {

                        Request request = new RequestImpl(method, url, httpVersion, headersMap, queryParams, params, (InetSocketAddress) socket.getRemoteSocketAddress(), bodyBytes, Server.getInstance());

                        Response response = new ResponseImpl();
                        System.out.println("Query Params: " + request.body());
//                        System.out.println("contentType: " + request.contentType());
//                        System.out.println("contentLength: " + headersMap);
//                        System.out.println("contentLength2: " + response.getHeaders());
//                        System.out.println("HTTP Version: " + httpVersion);
//                        System.out.println("Query Params: " + queryParams);
//                        System.out.println("Params: " + params);
//                        System.out.println("Body: " + new String(bodyBytes));
//                        Session session = YourRunnable.session(request, response,isHttps);
                        ((ResponseImpl) response).setOutputStream(os);
                        Object result = route.handle(request, response);
                        response.body(result.toString());//1.why always in result.body?
                        sendResponse(os, response);
//                        System.out.println("Status Code: " + response.getStatusCode());
//                        System.out.println("Status Message: " + response.getStatusMessage());
//
//                        // 打印响应头信息
//                        System.out.println("Headers:");
//                        for (AbstractMap.SimpleEntry<String, String> header : response.getHeaders()) {
//                            System.out.println(header.getKey() + ": " + header.getValue());
//                        }
//
//                        // 打印内容类型
//                        System.out.println("Content Type: " + response.getContentType());
//
//                        // 打印响应正文（以字节数组形式打印）
//                        byte[] bodyBytes2 = response.getBody();
//                        if (bodyBytes2 != null) {
//                            System.out.println("Body (as bytes): " + new String(bodyBytes2)); // 转换为字符串打印
//                        } else {
//                            System.out.println("Body is null");
//                        }
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorResponse(os, 500, "Internal Server Error", keepAlive);
                        continue;
                    }
                }

                // Handle static files if no route matched
                if (method.equals("POST") || method.equals("PUT")) {
                    sendErrorResponse(os, 405, "Method Not Allowed", keepAlive);
                } else if (url.contains("..")) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else if (!requestFile.exists()) {
                    sendErrorResponse(os, 404, "File Not Found", keepAlive);
                } else if (!requestFile.canRead()) {
                    sendErrorResponse(os, 403, "Forbidden", keepAlive);
                } else {
                    // Serve static file
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile))) {
                        byte[] fileContent = bis.readAllBytes();
                        Response response = new ResponseImpl();
                        response.bodyAsBytes(fileContent);
                        response.type(judgeContentType(requestFile.getAbsolutePath()));
                        response.status(200, "OK");
                        sendResponse(os, response);
                    } catch (Exception e) {
                        e.printStackTrace(); // Log exception for debugging
                        sendErrorResponse(os, 500, "Internal Server Error", keepAlive);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


//    private byte[] readRequestBody(InputStream is, int contentLength) throws IOException {
//        if (contentLength > 0) {
//            String str = new byte[contentLength];
//            int bytesRead = 0;
//            while (bytesRead < contentLength) {
//                int result = is.read(body, bytesRead, contentLength - bytesRead);
//                if (result == -1) {
//                    throw new IOException("Premature end of stream");
//                }
//                bytesRead += result;
//            }
//            return body;
//        }
//        return new byte[0];
//    }


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
    private Map<String, String> matchPath(String pathPattern, String url) {
        String[] patternParts = pathPattern.split("/");
        String[] urlParts = url.split("/");

        if (patternParts.length != urlParts.length) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < patternParts.length; i++) {
            if (patternParts[i].startsWith(":")) {
                params.put(patternParts[i].substring(1), urlParts[i]);
            } else if (!patternParts[i].equals(urlParts[i])) {
                return null;
            }
        }
        return params;
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