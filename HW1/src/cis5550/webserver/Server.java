package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Invalid amount of parameters. Expected two, but got " + args.length);
            System.out.println("Written by Tianshi Miao");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String directory = args[1];
        File directoryFile = new File(directory);
        if (!directoryFile.exists() || !directoryFile.isDirectory()) {
            System.out.println("Directory path " + directory + " is not valid");
            return;
        }

        ServerSocket ss = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        Socket socket = ss.accept();
        System.out.println("Accepted connection from " + socket.getInetAddress());
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        boolean keepAlive = true;

        do {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder headerBuilder = new StringBuilder();
            while (!(line = reader.readLine()).isEmpty()) {
                headerBuilder.append(line).append("\r\n");
            }
            headerBuilder.append("\r\n");

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
            if (method.equals("POST") || method.equals("PUT")) {
                sendErrorResponse(os, 405, "POST or PUT are not allowed", keepAlive);
            }
            else if (!httpVersion.equals("HTTP/1.1")) {
                sendErrorResponse(os, 505, "HTTP Version Not Supported", keepAlive);
            }
            else if (uri.contains("..")) {
                sendErrorResponse(os, 403, "Forbidden", keepAlive);
            }
            else if (!method.equals("GET") && !method.equals("HEAD")) {
                sendErrorResponse(os, 501, "Not Implemented", keepAlive);
            }
            else if (!requestFile.exists()) {
                sendErrorResponse(os, 404, "File Not Found", keepAlive);
            }
            else if (!requestFile.canRead()) {
                sendErrorResponse(os, 403, "Forbidden", keepAlive);
            }
            else {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(requestFile));
                BufferedOutputStream bos = new BufferedOutputStream(os);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

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
            } while (keepAlive);

            socket.close();
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
        bw.write("\r\n");
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
