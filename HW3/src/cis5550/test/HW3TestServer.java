package cis5550.test;

import java.nio.file.*;
import java.io.*;

import static cis5550.webserver.Server.*;

import cis5550.webserver.Session;

public class HW3TestServer {
    public static void main(String args[]) throws Exception {
        port(8080);
        securePort(443);

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
