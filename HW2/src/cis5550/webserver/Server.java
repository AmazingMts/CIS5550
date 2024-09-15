package cis5550.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import cis5550.webserver.Route;
import cis5550.webserver.YourRunnable;
public class Server {
    private static Server Serverinstance=null;
    private static boolean isRunning=false;
    private int port;
    private String directory;
    public static final Map<String, Route> getRoutes = new HashMap<>();
    public static final Map<String, Route> postRoutes = new HashMap<>();
    public static final Map<String, Route> putRoutes = new HashMap<>();
    public static Server getInstance() {
        if (Serverinstance == null) {
            Serverinstance = new Server();
        }
        return Serverinstance;
    }
    public void run(){
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = ss.accept();
                new Thread(new YourRunnable(socket, directory)).start();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static class staticFiles {
        public static void location(String s) {
            if(Serverinstance==null){
                Serverinstance=new Server();
            }
            Serverinstance.directory=s;
        }
    }
    public static void get(String path,Route route) {
        checkAndStartServer();
        getRoutes.put(path, route);
    }
    public static void post(String path,Route route) {
        checkAndStartServer();
        postRoutes.put(path, route);
    }
    public static void put(String path,Route route) {
        checkAndStartServer();
        putRoutes.put(path, route);
    }
    public static void port(int port) {
        if(Serverinstance==null){
            Serverinstance=new Server();
        }
        Serverinstance.port=port;
    }
    static void checkAndStartServer(){
        if(Serverinstance==null){
            Serverinstance=new Server();
        }
        if(!isRunning){
            isRunning=true;
            new Thread(()->Serverinstance.run()).start();
        }
    }
}