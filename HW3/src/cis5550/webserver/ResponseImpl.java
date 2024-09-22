package cis5550.webserver;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;

public class ResponseImpl implements Response {
    private byte[] body;  // 直接将字符串转换为字节数组;
    private boolean written = false;
    private boolean responseCommitted = false;
    private List<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
    private String contentType = "text/html";
    private int statusCode = 200;
    private String reasonPhrase = "OK";
    boolean halted = false;
    int haltStatusCode;
    String haltReasonPhrase;
    private OutputStream outputStream;

    @Override
    public void body(String body) {
        if (!written) {
            this.body = body.getBytes();
        }
    }

    @Override
    public void bodyAsBytes(byte[] bodyArg) {
        if (!written) {
            this.body = bodyArg;
        }
    }

    @Override
    public void header(String name, String value) {
        if (!written) {
            headers.add(new AbstractMap.SimpleEntry<>(name, value));
        }
    }

    @Override
    public void type(String contentType) {
        if (!written) {
            this.contentType = contentType;
        }
    }

    @Override
    public void status(int statusCode, String reasonPhrase) {
        if (!written) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (halted) {
            // 如果请求被停止，写出停止响应
            outputStream.write(("HTTP/1.1 " + haltStatusCode + " " + haltReasonPhrase + "\r\n").getBytes());
            outputStream.write("Connection: close\r\n".getBytes());
            outputStream.write("\r\n".getBytes());
            outputStream.flush();
            return;
        }

        if (!responseCommitted)
        {
            responseCommitted = true;
            written = true;

            // 写入响应头部信息
            outputStream.write(("HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n").getBytes());
            outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes());

            // 计算 Content-Length
            int contentLength = (body != null ? body.length : 0) + b.length;
            outputStream.write(("Content-Length: " + contentLength + "\r\n").getBytes());

            // 添加其他头部信息
            for (AbstractMap.SimpleEntry<String, String> header : headers) {
                outputStream.write((header.getKey() + ": " + header.getValue() + "\r\n").getBytes());
            }

            outputStream.write("Connection: close\r\n".getBytes());
            outputStream.write("\r\n".getBytes());
            System.out.println("Writing response headers:");
            System.out.println("Status: " + statusCode + " " + reasonPhrase);
            System.out.println("Content-Type: " + contentType);
            System.out.println("Content-Length: " + contentLength);
            for (AbstractMap.SimpleEntry<String, String> header : headers) {
                System.out.println(header.getKey() + ": " + header.getValue());
        }

        // 写入实际的响应体
        if (body != null) {
            System.out.println("Writing response body: " + new String(body));
            outputStream.write(body);

        }

        }
        outputStream.write(b);
        outputStream.flush();
    }


    @Override
    public void redirect(String url, int responseCode) {
        if (!written) {
            this.statusCode = responseCode;
            this.reasonPhrase = getReasonPhraseForCode(responseCode);
            this.body = null;
            header("Location", url);
        }
    }

    private String getReasonPhraseForCode(int statusCode) {
        switch (statusCode) {
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 307: return "Temporary Redirect";
            case 308: return "Permanent Redirect";
            default: return "Unknown Status";
        }
    }
    @Override
    public void halt(int statusCode, String reasonPhrase) {
        this.halted = true;
        this.haltStatusCode = statusCode;
        this.haltReasonPhrase = reasonPhrase;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getStatusMessage() {
        return reasonPhrase;
    }
    @Override
    public List<AbstractMap.SimpleEntry<String, String>> getHeaders() {
        return headers;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
    public byte[] getBody() {
        return body;
    }
    public void setOutputStream(OutputStream os) {
        this.outputStream = os;
    }
}
