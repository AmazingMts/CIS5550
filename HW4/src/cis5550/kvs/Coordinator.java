package cis5550.kvs;
import cis5550.webserver.Server;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static cis5550.webserver.Server.*;

public class Coordinator extends cis5550.generic.Coordinator {
    private static ConcurrentHashMap<String, WorkerInfo> workers = new ConcurrentHashMap<>();//define worker<id,workerinfo>
    private static class WorkerInfo {
        String ip;
        String port;
        Instant lastPing;
        WorkerInfo(String ip, String port) {
            this.ip = ip;
            this.port = port;
            this.lastPing = Instant.now();
        }
        public boolean isTimedOut() {
            return Instant.now().minusSeconds(15).isAfter(lastPing);
        }
    }
    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("invalid arguments");
            return;
        }
        int portNumber;
        portNumber = Integer.parseInt(args[0]);
        Server.port(portNumber);
        Coordinator.registerRoutes();
        new Thread(() -> {//delete inactive workers
            while (true) {
                try {
                    Thread.sleep(1000);
                    workers.entrySet().removeIf(entry -> entry.getValue().isTimedOut());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        get("/ping", (req, res) -> {//create a worker or update a worker
            String workerId = req.queryParams("id");
            String workerPort = req.queryParams("port");
            String workerIp = req.ip();
            if (workerId == null || workerPort == null) {
                res.status(400,"inValid request");
                return "Missing ID or port number.";
            }

            WorkerInfo worker = workers.get(workerId);
            if(worker!=null ) {
                worker.lastPing = Instant.now();
            }else {
                workers.put(workerId, new WorkerInfo(workerIp, workerPort));
            }
            res.status(200,"successful");
            return "OK";  // 返回 "OK" 响应
        });
        get("/",(req,res)->{//show active workers
            StringBuilder html=new StringBuilder();
            html.append("<html><head>Active Workers</head><body>");
            html.append("<body>Active Workers");
            html.append("<table border='1'><tr><th>ID</th><th>IP Address</th><th>Port</th><th>link</th></tr>");
            for (String workId:workers.keySet()){
                WorkerInfo worker = workers.get(workId);
                String Id=workId;
                String IP=worker.ip;
                String port=worker.port;
                String link="http://"+IP+":"+port+"/";
                html.append("<tr><td>").append(Id).append("</td><td>").append(IP).append("</td><td>").append(port).append("</td><td>").append(link).append("</td></tr>");
            }
            html.append("</table>").append(workerTable()).append("</body></html>");
            res.body(html.toString());
            return html;
        });
        get("/workers",(req,res)->{//show active workers
            StringBuilder result=new StringBuilder();
           int ActiveWorker=workers.size();
           result.append(ActiveWorker).append("\n");
           for (String workerId : workers.keySet()){
               WorkerInfo worker = workers.get(workerId);
               result.append(workerId).append(",").append(worker.ip).append(":").append(worker.port).append("\n");
           }
           return result.toString();
        });

    }
}
