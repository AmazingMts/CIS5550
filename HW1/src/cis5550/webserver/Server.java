package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cis5550.tools.Logger;

public class Server
{
	private static final Logger logger = Logger.getLogger(Server.class);
	// use threadPool realize concurrency
	private static ExecutorService threadPool;

	public static void main(String[] args) throws IOException {
		// Check for correct number of arguments
		if (args.length != 2)
		{
			logger.error("Invalid amount of parameters. Expected two, but got " + args.length);
			System.out.println("Written by Tianshi Miao");
			return;
		}
		// Verify whether port and directory exist
		int port = Integer.parseInt(args[0]);
		String directory = args[1];
		File directoryFile = new File(directory);
		if (!directoryFile.exists() || !directoryFile.isDirectory())
		{
			logger.error("Directory path " + directory + " is not valid");
			return;
		}
		// Create ServerSocket
		ServerSocket ss = new ServerSocket(port);
		threadPool = Executors.newFixedThreadPool(999);
		logger.info("Server started on port " + port);

		// listen for client connections
		while (true) // while can ensure continuous working, but once can only listen one client
		{
			// Accept client connection
			Socket socket = ss.accept();
			// Handle client request in a new thread
			threadPool.submit(() -> {
				try {
					logger.info("Accepted connection from " + socket.getInetAddress());

					// open tube for client request and send response
					InputStream is = socket.getInputStream();
					OutputStream os = socket.getOutputStream();
					// read input data using byte， use ByteArrayOutputStream to write byte data into memory with bytearray
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] bytes = new byte[1024];
					int b;
					// write byte Stream into byte array
					while ((b = is.read(bytes)) != -1) {
						baos.write(bytes, 0, b);
						byte[] data = baos.toByteArray();
						// as the boundary is r,n=13,10
						if (data.length > 4 &&
								data[data.length - 4] == 13 && // r
								data[data.length - 3] == 10 && // n
								data[data.length - 2] == 13 && // r
								data[data.length - 1] == 10) { // n
							break;
						}
					}
					// Convert bytes to string and parse firstline and header
					String head = new String(baos.toByteArray(), "UTF-8");
					BufferedReader br = new BufferedReader(new StringReader(head));
					int content_Length = 0;
					String headerString;
					String FirstLine = br.readLine(); // use to error handling
					String[] firstline = FirstLine.split(" ");
					String method = firstline[0];
					String URI = firstline[1];
					String http_version = firstline[2];
					String RequestFilepath = directory + URI;
					File RequestFile = new File(RequestFilepath);

					if (FirstLine == null || FirstLine.isEmpty()) {
						// examine first line of header is null or not
						sendErrorResponse(os, 400, "bad request");
						socket.close();
						return;
					}
					if (firstline.length != 3) { // examine the component of the first line is valid or not
						sendErrorResponse(os, 400, "bad request");
						socket.close();
						return;
					}
					// use else-if means when client meets first problem, return alert and do not continue
					if (method.equals("POST") || method.equals("PUT")) {
						sendErrorResponse(os, 405, "POST or PUT are not allowed");
						socket.close();
						return;
					}
					if (!http_version.equals("HTTP/1.1")) {
						sendErrorResponse(os, 505, "Version Not Supported if the protocol is anything other than HTTP/1.1");
						socket.close();
						return;
					}
					if (URI.contains("..")) {
						sendErrorResponse(os, 403, "Forbidden");
						socket.close();
						return;
					}
					if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST") && !method.equals("PUT")) {
						sendErrorResponse(os, 501, "invalid method");
						socket.close();
						return;
					}
					if (!RequestFile.exists()) {
						sendErrorResponse(os, 404, "File Not Found");
						socket.close();
						return;
					}
					if (!RequestFile.canRead()) {
						sendErrorResponse(os, 403, "Permission denied");
						socket.close();
						return;
					}
					// parse header
					Map<String, String> headers = new HashMap<>();
					String headerLine;
					while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
						logger.info("Header: " + headerLine);
						// input the name and content in map
						String[] headerParts = headerLine.split(":", 2);
						if (headerParts.length == 2) {
							String name = headerParts[0].trim().toLowerCase(); // 转换为小写
							String value = headerParts[1].trim();
							headers.put(name, value);
						}
					}

					if (content_Length > 0) {
						byte[] content = new byte[content_Length];
						int bytesRead = is.read(content, 0, content_Length); // Read the exact number of bytes
						System.out.println("Message Body (bytes): " + new String(content)); // Optional: log the body as a string
					}

					// Send true HTTP response to the client
					PrintWriter pw = new PrintWriter(os, true);
					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-Type: " + judgeContentType(RequestFilepath));
					pw.println("Server: TianshiServer");
					pw.println("Content-Length: " + RequestFile.length());
					pw.println();

					FileInputStream fis = new FileInputStream(RequestFile);
					BufferedInputStream bis = new BufferedInputStream(fis);
					byte[] fileTransfer = new byte[1024];
					int fileTransfernum;
					while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
						os.write(fileTransfer, 0, fileTransfernum);
					}
					os.flush();
					socket.close();
				} catch (IOException e) {
					logger.error("Error handling client request", e);
				} finally {
					try {
						socket.close();
					} catch (IOException e) {
						logger.error("Error closing socket", e);
					}
				}
			});
		}
	}

	private static void sendErrorResponse(OutputStream os, int statusCode, String statusMessage) throws IOException {
		PrintWriter pw = new PrintWriter(os);
		pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
		pw.println("Content-Type: text/plain");
		pw.println("Content-Length: " + statusMessage.length());
		pw.println(); // End of headers
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
