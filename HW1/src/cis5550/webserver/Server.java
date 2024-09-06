package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

	public static void main(String[] args) throws IOException {
		// Check for correct number of arguments
		if (args.length != 2) {
			System.out.println("Invalid amount of parameters. Expected two, but got " + args.length);
			System.out.println("Written by Tianshi Miao");
			return;
		}

		// Verify whether port and directory exist
		int port = Integer.parseInt(args[0]);
		String directory = args[1];
		File directoryFile = new File(directory);
		if (!directoryFile.exists() || !directoryFile.isDirectory()) {
			System.out.println("Directory path " + directory + " is not valid");
			return;
		}

		// Create ServerSocket
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("Server started on port " + port);

		// Listen for client connections
		while (true) {
			// Accept client connection
			Socket socket = serverSocket.accept();
			System.out.println("Accepted connection from " + socket.getInetAddress());

			// Open streams for client request and response
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();

			// Read the input data, searching for the end of the headers (double CRLF)
			StringBuilder headersBuilder = new StringBuilder();
			int prevByte = -1;
			int currByte = -1;
			boolean foundDoubleCRLF = false;

			while ((currByte = is.read()) != -1) {
				headersBuilder.append((char) currByte);

				// Check if we have reached the double CRLF (13, 10, 13, 10)
				if (prevByte == 13 && currByte == 10) {
					if (headersBuilder.length() >= 4 &&
							headersBuilder.charAt(headersBuilder.length() - 4) == 13 &&
							headersBuilder.charAt(headersBuilder.length() - 3) == 10 &&
							headersBuilder.charAt(headersBuilder.length() - 2) == 13 &&
							headersBuilder.charAt(headersBuilder.length() - 1) == 10) {
						foundDoubleCRLF = true;
						break;
					}
				}
				prevByte = currByte;
			}

			// If we didn't find the double CRLF, it's an invalid request
			if (!foundDoubleCRLF) {
				sendErrorResponse(os, 400, "Bad Request");
				socket.close();
				continue;
			}

			// Parse the headers
			String head = headersBuilder.toString();
			BufferedReader br = new BufferedReader(new StringReader(head));

			// Read the first line (Request-Line)
			String firstLine = br.readLine();
			if (firstLine == null || firstLine.isEmpty()) {
				sendErrorResponse(os, 400, "Bad Request");
				socket.close();
				continue;
			}

			String[] firstLineParts = firstLine.split(" ");
			if (firstLineParts.length != 3) {
				sendErrorResponse(os, 400, "Bad Request");
				socket.close();
				continue;
			}

			String method = firstLineParts[0];
			String uri = firstLineParts[1];
			String httpVersion = firstLineParts[2];

			if (!httpVersion.equals("HTTP/1.1")) {
				sendErrorResponse(os, 505, "HTTP Version Not Supported");
				socket.close();
				continue;
			}

			// Check if the URI is valid
			if (uri.contains("..")) {
				sendErrorResponse(os, 403, "Forbidden");
				socket.close();
				continue;
			}

			// Only support GET and HEAD methods
			if (!method.equals("GET") && !method.equals("HEAD")) {
				sendErrorResponse(os, 501, "Not Implemented");
				socket.close();
				continue;
			}

			// Handle file request
			File requestedFile = new File(directory + uri);
			if (!requestedFile.exists()) {
				sendErrorResponse(os, 404, "Not Found");
				socket.close();
				continue;
			}

			if (!requestedFile.canRead()) {
				sendErrorResponse(os, 403, "Forbidden");
				socket.close();
				continue;
			}

			// Send response
			sendResponse(os, 200, "OK", requestedFile);
			socket.close();
		}
	}

	private static void sendErrorResponse(OutputStream os, int statusCode, String statusMessage) throws IOException {
		PrintWriter pw = new PrintWriter(os);
		pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
		pw.println("Content-Type: text/plain");
		pw.println("Content-Length: " + statusMessage.length());
		pw.println(); // End of headers (two CRLFs)
		pw.println(statusMessage);
		pw.flush(); // Ensure all data is sent
	}

	private static void sendResponse(OutputStream os, int statusCode, String statusMessage, File file) throws IOException {
		PrintWriter pw = new PrintWriter(os);
		pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
		pw.println("Content-Type: " + judgeContentType(file.getName()));
		pw.println("Content-Length: " + file.length());
		pw.println(); // End of headers (two CRLFs)
		pw.flush(); // Ensure headers are sent before body

		// Send the file content
		try (FileInputStream fis = new FileInputStream(file);
			 BufferedInputStream bis = new BufferedInputStream(fis)) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = bis.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush(); // Ensure all file data is sent
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
