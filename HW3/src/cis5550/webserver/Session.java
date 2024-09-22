package cis5550.webserver;

public interface Session {
  // Returns the session ID (the value of the SessionID cookie) that this session is associated with
  String id();

  // The methods below return the time this session was created, and the time time this session was
  // last accessed. The return values should be in the same format as the return value of 
  // System.currentTimeMillis().
  long creationTime();
  long lastAccessedTime();

  // Set the maximum time, in seconds, this session can be active without being accessed.
  void maxActiveInterval(int seconds);

  // Invalidates the session. You do not need to delete the cookie on the client when this method
  // is called; it is sufficient if the session object is removed from the server.
  void invalidate();

  // The methods below look up the value for a given key, and associate a key with a new value,
  // respectively.
  Object attribute(String name);
  void attribute(String name, Object value);
  int getmaxActiveInterval();
};
