package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import cis5550.webserver.ResponseImpl;
import cis5550.webserver.Response;

public class Server {
    private static Server Serverinstance = null;
    private static boolean isRunning = false;
    private int port;
    public static String directory;
    public static final Map<String, Route> getRoutes = new HashMap<>();
    public static final Map<String, Route> postRoutes = new HashMap<>();
    public static final Map<String, Route> putRoutes = new HashMap<>();
//    private static Map<String,Session> sessions = new HashMap<>();
//    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@";
//    private static final int SESSION_ID_LENGTH = 20;


    public static Server getInstance() {
        if (Serverinstance == null) {
            Serverinstance = new Server();
        }
        return Serverinstance;
    }

    public void run() {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("HTTP Server started on port " + port);
            while (true) {
                Socket socket = ss.accept();
                new Thread(new YourRunnable(socket, directory)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class staticFiles {
        public static void location(String s) {
            if (Serverinstance == null) {
                Serverinstance = new Server();
            }
            Serverinstance.directory = s;
        }
    }

    public static void get(String path, Route route) {
        checkAndStartServer();
        getRoutes.put(path, route);
    }

    public static void post(String path, Route route) {
        checkAndStartServer();
        postRoutes.put(path, route);
    }

    public static void put(String path, Route route) {
        checkAndStartServer();
        putRoutes.put(path, route);
    }

    public static void port(int port) {
        if (Serverinstance == null) {
            Serverinstance = new Server();
        }
        Serverinstance.port = port;
        checkAndStartServer();
    }

    public static void securePort(int securePortNo) {
        new Thread(() -> {
            try {
                String pwd = "secret";
                KeyStore keyStore = KeyStore.getInstance("JKS");
                keyStore.load(new FileInputStream("keystore.jks"), pwd.toCharArray());

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keyStore, pwd.toCharArray());

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

                SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
                SSLServerSocket serverSocketTLS = (SSLServerSocket) factory.createServerSocket(securePortNo);
                System.out.println("HTTPS Server started on port " + securePortNo);
                while (true) {
                    Socket socket = serverSocketTLS.accept();
                    new Thread(new YourRunnable(socket, directory)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    static void checkAndStartServer() {
        if (Serverinstance == null) {
            Serverinstance = new Server();
        }
        if (!isRunning) {
            isRunning = true;
            new Thread(() -> Serverinstance.run()).start();
        }
    }
//    public Session session(Request req, Response res) {
//        String sessionId = getSessionIdFromCookie(req);
//        SessionImpl session;
//        System.out.println("Session ID: " + sessionId);
//        System.out.println("Headers before adding Set-Cookie: " + res.getHeaders());
//
//        // 如果有有效的 SessionID，获取对应会话
//        if (sessionId != null && sessions.containsKey(sessionId)) {
//            session = (SessionImpl) sessions.get(sessionId);
//            session.lastAccessedTime=System.currentTimeMillis();
//        } else {
//            session = new SessionImpl(generateSessionId());
//            sessions.put(session.id(), session);
//            res.header("Set-Cookie", "SessionID=" + session.id() + "; HttpOnly");
//            System.out.println("Headers after adding Set-Cookie: " + res.getHeaders());
//        }
//        return session;
//    }
//    private String getSessionIdFromCookie(Request req) {
//        String cookieHeader = req.headers("Cookie");
//        if (cookieHeader != null) {
//            String[] cookies = cookieHeader.split(";");
//            for (String cookie : cookies) {
//                String[] cookiePair = cookie.trim().split("=");
//                if (cookiePair.length == 2 && cookiePair[0].equals("SessionID")) {
//                    return cookiePair[1];
//                }
//            }
//        }
//        return null;
//    }
//    private String generateSessionId() {
//        SecureRandom random = new SecureRandom();
//        StringBuilder sessionId = new StringBuilder(SESSION_ID_LENGTH);
//
//        for (int i = 0; i < SESSION_ID_LENGTH; i++) {
//            int index = random.nextInt(CHARACTERS.length());
//            sessionId.append(CHARACTERS.charAt(index));
//        }
//
//        return sessionId.toString();
//    }

}
