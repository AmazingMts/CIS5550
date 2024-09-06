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
        while (true) {
            Socket socket = ss.accept();
            //
            new Thread(new YourRunnable(socket, directory)).start();
        }
    }

}


