package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cis5550.tools.Logger;

public class Server {
	private static final Logger logger = Logger.getLogger(Server.class);
	private static ExecutorService threadPool;

	public static void main(String[] args) throws IOException {
		// Check for correct number of arguments
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

		// Create ServerSocket
		ServerSocket ss = new ServerSocket(port);
		threadPool = Executors.newFixedThreadPool(100);
		logger.info("Server started on port " + port);

		// Listen for client connections
		while (true) {
			Socket socket = ss.accept();
			threadPool.submit(() -> handleRequest(socket, directory));
		}
	}

	private static void handleRequest(Socket socket, String directory) {
		try (InputStream is = socket.getInputStream();
			 OutputStream os = socket.getOutputStream();
			 BufferedReader br = new BufferedReader(new InputStreamReader(is));
			 PrintWriter pw = new PrintWriter(os, true)) {

			logger.info("Accepted connection from " + socket.getInetAddress());

			// Read the request line
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

			// Check for valid HTTP version
			if (!httpVersion.equals("HTTP/1.1")) {
				sendErrorResponse(os, 505, "HTTP Version Not Supported");
				return;
			}

			// Check for valid method
			if (method.equals("POST") || method.equals("PUT")) {
				sendErrorResponse(os, 405, "Method Not Allowed");
				return;
			}

			// Check for forbidden URI
			if (uri.contains("..")) {
				sendErrorResponse(os, 403, "Forbidden");
				return;
			}

			File requestFile = new File(directory, uri);
			if (!requestFile.exists()) {
				sendErrorResponse(os, 404, "Not Found");
				return;
			}
			if (!requestFile.canRead()) {
				sendErrorResponse(os, 403, "Forbidden");
				return;
			}

			// Read headers (until an empty line is encountered)
			Map<String, String> headers = new HashMap<>();
			String headerLine;
			while ((headerLine = br.readLine()) != null && !headerLine.trim().isEmpty()) {
				logger.info("Header: " + headerLine);
				String[] headerParts = headerLine.split(":", 2);
				if (headerParts.length == 2) {
					headers.put(headerParts[0].trim().toLowerCase(), headerParts[1].trim());
				}
			}

			// If no empty line (CRLF) is found, return Bad Request
			if (headerLine == null || headerLine.isEmpty()) {
				sendErrorResponse(os, 400, "Bad Request");
				return;
			}

			// Read request body if needed
			int contentLength = 0;
			if (headers.containsKey("content-length")) {
				try {
					contentLength = Integer.parseInt(headers.get("content-length"));
				} catch (NumberFormatException e) {
					sendErrorResponse(os, 400, "Bad Request");
					return;
				}
			}

			// Read request body
			byte[] body = new byte[contentLength];
			if (contentLength > 0) {
				int bytesRead = 0;
				while (bytesRead < contentLength) {
					int result = is.read(body, bytesRead, contentLength - bytesRead);
					if (result == -1) break;
					bytesRead += result;
				}
				logger.info("Request Body (bytes): " + new String(body));
			}

			// Send response
			pw.println("HTTP/1.1 200 OK");
			pw.println("Content-Type: " + judgeContentType(requestFile.getPath()));
			pw.println("Content-Length: " + requestFile.length());
			pw.println("Server: TianshiServer");
			pw.println();
			pw.flush();

			// Send file content
			try (FileInputStream fis = new FileInputStream(requestFile);
				 BufferedInputStream bis = new BufferedInputStream(fis)) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = bis.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				os.flush();
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
