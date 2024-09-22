package cis5550.webserver;

import java.util.HashMap;
import java.util.Map;

public class SessionImpl implements Session {
    private String id;
    private final long creationTime;
    long lastAccessedTime;
    private int maxActiveInterval; // in seconds
    private final Map<String, Object> attributes;
    private boolean isPermanent;
    public SessionImpl(String sessionId) {
        this.id = sessionId;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = System.currentTimeMillis();
        this.maxActiveInterval = 300;
        this.attributes = new HashMap<>();
        this.isPermanent = true;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public long creationTime() {
        return creationTime;
    }

    @Override
    public long lastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public void maxActiveInterval(int seconds) {
        this.maxActiveInterval = seconds;
    }

    @Override
    public void invalidate() {
        // Optionally clear attributes or perform any cleanup if necessary
        attributes.clear();
        // Mark this session as invalidated
        this.id = null;
    }

    @Override
    public Object attribute(String name) {
        lastAccessedTime = System.currentTimeMillis(); // Update last accessed time
        return attributes.get(name);
    }

    @Override
    public void attribute(String name, Object value) {
        lastAccessedTime = System.currentTimeMillis(); // Update last accessed time
        attributes.put(name, value);
    }

    @Override
    public int getmaxActiveInterval() {
        return maxActiveInterval;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

}
