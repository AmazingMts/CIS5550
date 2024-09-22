package cis5550.webserver;

import static cis5550.webserver.Server.*;

public class Main {
    public static void main(String[] args) throws Exception {
        port(8080);
        securePort(443);
        get("/", (req, res) -> {
            return "Welcome to test Server";});
        get("/echo/:x", (req, res) -> {
            return req.params("x");
        });
        get("/session", (req, res) -> {
            Session s = req.session();
            if (s == null) return "null";
            return s.id();
        });
        get("/perm/:x", (req, res) -> {
            Session s = req.session();
            s.maxActiveInterval(1);
            if (s.attribute("test") == null) s.attribute("test", req.params("x"));
            return s.attribute("test");
        });

    }
}

