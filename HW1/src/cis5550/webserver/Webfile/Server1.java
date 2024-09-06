import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import cis5550.tools.Logger;

public class SimpleServer {
    private static final Logger logger = Logger.getLogger(SimpleServer.class);
    private static final int CR = 13;
    private static final int LF = 10;
    private static final byte[] CRLF = {CR, LF, CR, LF};

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("苗天石");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String rootDirectory = args[1];

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server is listening on port " + port);

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    logger.info("Client connected from " + socket.getRemoteSocketAddress());
                    handleRequest(socket, rootDirectory);
                } catch (Exception e) {
                    logger.error("Exception occurred while accepting connection: ", e);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while setting up server: ", e);
        }
    }

    private static void handleRequest(Socket socket, String rootDirectory) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] requestBytes = readRequestBytes(inputStream);

            // Convert bytes to string and parse headers
            String requestString = new String(requestBytes);
            boolean isError = false;
            String errorMessage = "";
            String filePath = "";

            // Determine the response based on the request
            if (!isValidRequest(requestString)) {
                isError = true;
                errorMessage = "400 Bad Request";
            } else if (!isValidMethod(requestString)) {
                isError = true;
                errorMessage = "405 Method Not Allowed";
            } else if (!isValidProtocol(requestString)) {
                isError = true;
                errorMessage = "505 HTTP Version Not Supported";
            } else {
                filePath = extractFilePath(requestString);
                if (filePath.contains("..")) {
                    isError = true;
                    errorMessage = "403 Forbidden";
                } else {
                    File file = new File(rootDirectory, filePath);
                    if (!file.exists()) {
                        isError = true;
                        errorMessage = "404 Not Found";
                    } else if (!file.canRead()) {
                        isError = true;
                        errorMessage = "403 Forbidden";
                    }
                }
            }

            if (isError) {
                sendErrorResponse(socket, errorMessage);
            } else {
                sendFileResponse(socket, filePath, rootDirectory);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while handling request: ", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Exception occurred while closing socket: ", e);
            }
        }
    }

    private static byte[] readRequestBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int prevByte = -1;
        int currentByte;

        while ((currentByte = inputStream.read()) != -1) {
            byteArrayOutputStream.write(currentByte);
            if (prevByte == CR && currentByte == LF) {
                byte[] bytes = byteArrayOutputStream.toByteArray();
                // Check if last bytes are CRLFCRLF
                if (bytes.length >= 4 && bytes[bytes.length - 4] == CR && bytes[bytes.length - 3] == LF &&
                        bytes[bytes.length - 2] == CR && bytes[bytes.length - 1] == LF) {
                    return bytes;
                }
            }
            prevByte = currentByte;
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static boolean isValidRequest(String requestString) {
        return requestString.contains("GET") || requestString.contains("HEAD");
    }

    private static boolean isValidMethod(String requestString) {
        return requestString.contains("GET") || requestString.contains("HEAD");
    }

    private static boolean isValidProtocol(String requestString) {
        return requestString.contains("HTTP/1.1");
    }

    private static String extractFilePath(String requestString) {
        String[] lines = requestString.split("\r\n");
        if (lines.length > 0) {
            String[] requestLineParts = lines[0].split(" ");
            if (requestLineParts.length > 1) {
                return requestLineParts[1];
            }
        }
        return "";
    }

    private static void sendErrorResponse(Socket socket, String errorMessage) throws Exception {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            writer.println("HTTP/1.1 " + errorMessage);
            writer.println("Content-Type: text/plain");
            writer.println("Server: SimpleServer");
            writer.println("Content-Length: " + errorMessage.length());
            writer.println(); // Empty line to end headers
            writer.println(errorMessage);
            writer.flush();
        }
    }

    private static void sendFileResponse(Socket socket, String filePath, String rootDirectory) throws Exception {
        File file = new File(rootDirectory, filePath);
        String contentType = "application/octet-stream";
        if (filePath.endsWith(".txt")) {
            contentType = "text/plain";
        }
        // Add more content types if needed

        try (FileInputStream fileInputStream = new FileInputStream(file);
             OutputStream outputStream = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream))) {

            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + file.length());
            writer.println(); // Empty line to end headers
            writer.flush(); // Ensure headers are sent before file data

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush(); // Ensure all file data is sent
        } catch (IOException e) {
            logger.error("Error sending file: ", e);
            sendErrorResponse(socket, "500 Internal Server Error");
        }
    }
}
