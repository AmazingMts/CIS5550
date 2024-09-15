package cis5550.test;

import java.nio.file.*;
import java.io.*;

import static cis5550.webserver.Server.*;

public class HW2TestServer {
	public static void main(String args[]) throws Exception {
    port(8080);
    staticFiles.location("__test123");
    get("/hello", (req,res) -> { return "Hello World"; });
    get("/foo", (req, res) -> { res.status(789, "Foo"); return null; });
    post("/postbody", (req,res) -> { return "req.body() returned: "+req.body(); });
    put("/puthdr", (req,res) -> { if (req.headers("x-foo") == null) return "No X-Foo header found!"; res.header("X-Bar", req.headers("X-Foo")); return "OK"; });
    put("/pathpar/:blah/123/:blubb", (req,res) -> { return "Val1: ["+req.params("blah")+"] Val2: ["+req.params("blubb")+"]"; });
    get("/error", (req,res) -> { res.body("Should not show up!"); throw new Exception("Exception thrown on purpose (for testing)"); });
    get("/write", (req,res) -> { res.header("X-Bar", "present"); res.write("Hello ".getBytes()); res.write("World!".getBytes()); return null; });
    get("/qparam", (req,res) -> { return req.queryParams("par1")+","+req.queryParams("par2")+","+req.queryParams("par3")+","+req.queryParams("par4"); });
  }
}
