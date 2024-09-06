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
			// 处理客户端请求的线程
			threadPool.submit(() -> {
				try {
					logger.info("Accepted connection from " + socket.getInetAddress());

					// 打开输入输出流
					InputStream is = socket.getInputStream();
					OutputStream os = socket.getOutputStream();

					// 使用 ByteArrayOutputStream 读取输入数据
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] bytes = new byte[1024];
					int b;

					// 读取输入流直到头部结束（CRLF CRLF）
					while ((b = is.read(bytes)) != -1) {
						baos.write(bytes, 0, b);
						byte[] data = baos.toByteArray();
						// 检查 CRLF CRLF 边界
						if (data.length > 4 &&
								data[data.length - 4] == 13 && // CR
								data[data.length - 3] == 10 && // LF
								data[data.length - 2] == 13 && // CR
								data[data.length - 1] == 10) { // LF
							break;
						}
					}

					// 将字节转换为字符串并解析头部
					String head = new String(baos.toByteArray(), "UTF-8");
					BufferedReader br = new BufferedReader(new StringReader(head));

					// 解析第一行
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

					// 检查方法和版本
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

					// 解析头部
					Map<String, String> headers = new HashMap<>();
					String headerLine;
					while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
						logger.info("Header: " + headerLine);
						String[] headerParts = headerLine.split(":", 2);
						if (headerParts.length == 2) {
							String name = headerParts[0].trim().toLowerCase();
							String value = headerParts[1].trim();
							headers.put(name, value);
						}
					}

					// 处理 Content-Length（如果存在）
					int contentLength = 0;
					if (headers.containsKey("content-length")) {
						try {
							contentLength = Integer.parseInt(headers.get("content-length"));
						} catch (NumberFormatException e) {
							sendErrorResponse(os, 400, "Bad Request");
							return;
						}
					}

					// 如果 Content-Length 大于 0，则读取并丢弃消息体
					if (contentLength > 0) {
						byte[] content = new byte[contentLength];
						int bytesRead = is.read(content, 0, contentLength);
						if (bytesRead != contentLength) {
							sendErrorResponse(os, 400, "Bad Request");
							return;
						}
						// 可选：将消息体作为字符串记录
						System.out.println("Message Body (bytes): " + new String(content));
					}

					// 发送真实的 HTTP 响应给客户端
					PrintWriter pw = new PrintWriter(os, true);
					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-Type: " + judgeContentType(requestFilepath));
					pw.println("Server: TianshiServer");
					pw.println("Content-Length: " + requestFile.length());
					pw.println(); // 头部和主体之间的空行

					try (FileInputStream fis = new FileInputStream(requestFile);
						 BufferedInputStream bis = new BufferedInputStream(fis)) {
						byte[] fileTransfer = new byte[1024];
						int fileTransfernum;
						while ((fileTransfernum = bis.read(fileTransfer)) != -1) {
							os.write(fileTransfer, 0, fileTransfernum);
						}
						os.flush(); // 确保所有数据都被写出
					} catch (IOException e) {
						logger.error("Error sending file", e);
					}

				} catch (IOException e) {
					logger.error("Error handling client request", e);
				} finally {
					try {
						socket.close(); // 确保连接被关闭
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