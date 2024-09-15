package cis5550.webserver;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class ResponseImpl implements Response {
    private byte[] body;
    private boolean written = false;
    private boolean responseCommitted = false;
    private List<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
    private String contentType = "text/html";
    private int statusCode = 200;
    private String reasonPhrase = "OK";
    boolean halted = false;
    int haltStatusCode;
    String haltReasonPhrase;
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
    public void write(byte[] b) throws Exception {
        if (halted) {
            // If halted, write halt response instead of normal response
            System.out.write(("HTTP/1.1 " + haltStatusCode + " " + haltReasonPhrase + "\r\n").getBytes());
            System.out.write("Connection: close\r\n".getBytes());
            System.out.write("\r\n".getBytes());
            return;
        }

        if (!responseCommitted) {
            responseCommitted = true;
            written = true;

            System.out.write(("HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n").getBytes());

            System.out.write(("Content-Type: " + contentType + "\r\n").getBytes());

            System.out.write("Connection: close\r\n".getBytes());

            for (AbstractMap.SimpleEntry<String, String> header : headers) {
                System.out.write((header.getKey() + ": " + header.getValue() + "\r\n").getBytes());
            }

            System.out.write("\r\n".getBytes());
        }

        System.out.write(b);
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
}
