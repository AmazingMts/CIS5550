package cis5550.webserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Server {


	static final String SERVER_NAME = "TianshiServer";
	private static final int NUM_WORKERS = 100;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Written by [Your Full Name]");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		String directory = args[1];

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server started on port " + port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new Thread(new ClientHandler(clientSocket, directory)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ClientHandler implements Runnable {
	private Socket clientSocket;
	private String directory;

	public ClientHandler(Socket clientSocket, String directory) {
		this.clientSocket = clientSocket;
		this.directory = directory;
	}

	@Override
	public void run() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			 OutputStream out = clientSocket.getOutputStream()) {

			String requestLine = in.readLine();
			if (requestLine == null || requestLine.isEmpty()) {
				sendError(out, 400, "Bad Request");
				return;
			}

			String[] requestParts = requestLine.split(" ");
			if (requestParts.length != 3) {
				sendError(out, 400, "Bad Request");
				return;
			}

			String method = requestParts[0];
			String path = requestParts[1];
			String protocol = requestParts[2];

			if (!protocol.equals("HTTP/1.1")) {
				sendError(out, 505, "HTTP Version Not Supported");
				return;
			}

			if (!method.equals("GET") && !method.equals("HEAD")) {
				sendError(out, 405, "Method Not Allowed");
				return;
			}

			if (path.contains("..")) {
				sendError(out, 403, "Forbidden");
				return;
			}

			File file = new File(directory, path);
			if (!file.exists()) {
				sendError(out, 404, "Not Found");
				return;
			}

			if (!file.canRead()) {
				sendError(out, 403, "Forbidden");
				return;
			}

			sendResponse(out, file, method.equals("GET"));

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendError(OutputStream out, int statusCode, String statusMessage) throws IOException {
		String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
				"Content-Length: 0\r\n" +
				"Connection: close\r\n" +
				"\r\n";
		out.write(response.getBytes());
	}

	private void sendResponse(OutputStream out, File file, boolean includeBody) throws IOException {
		String contentType = getContentType(file);
		long contentLength = file.length();

		StringBuilder responseHeader = new StringBuilder();
		responseHeader.append("HTTP/1.1 200 OK\r\n")
				.append("Content-Type: ").append(contentType).append("\r\n")
				.append("Content-Length: ").append(contentLength).append("\r\n")
				.append("Server: ").append(Server.SERVER_NAME).append("\r\n")
				.append("\r\n");

		out.write(responseHeader.toString().getBytes());

		if (includeBody) {
			try (FileInputStream fileIn = new FileInputStream(file)) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = fileIn.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	private String getContentType(File file) {
		String fileName = file.getName();
		if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
			return "text/html";
		} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileName.endsWith(".txt")) {
			return "text/plain";
		} else {
			return "application/octet-stream";
		}
	}
}
