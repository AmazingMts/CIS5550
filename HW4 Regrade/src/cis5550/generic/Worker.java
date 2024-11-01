package cis5550.generic;
public class Worker {
    public static void startPingThread(){
        Thread pingThread = new Thread(()->{
            while (true);
        });
        pingThread.start();
    }
}
