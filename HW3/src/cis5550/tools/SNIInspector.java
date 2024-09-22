package cis5550.tools;

import javax.net.ssl.*;
import java.net.*;
import java.io.*;

// How to use this class:
//   1) Use a _regular_ ServerSocket (_not_ a SSLServerSocket) to accept the connection initially
//   2) Instantiate a SNIInspector and call parseConnection() on the Socket for the accepted connection
//   3) Call getHostName() to find out what hostname the client specified via SNI
//   4) Call getInputStream() to get a ByteArrayInputStream with the data that has been read from the connection thus far
//   5) Call createSocket() on the SSLContext's socketFactory, and provide as arguments 1) the Socket, 2) the input stream, and 3) 'true'.
// If all steps complete successfully, you can use the resulting Socket just as you would use a Socket that was accepted by the
// code in the handout.
//
// Basically, the code reads the first couple of bytes (the 'client hello') from the connection, inspects them to find the SNI
// extension (if there is one), extracts the hostname, and then returns an InputStream with the bytes that have already been
// read, so the createSocket() function can 'replay' them to restart the beginning of the handshake.

public class SNIInspector {
  SNIHostName hostName;
  ByteArrayInputStream bais;

  public SNIInspector() {
    hostName = null;
    bais = null;
  }

  public SNIHostName getHostName() {
    return hostName;
  }

  public ByteArrayInputStream getInputStream() {
    return bais;
  }

  boolean readToLength(InputStream ins, byte buffer[], int bytesSoFar, int bytesNeeded) {
    while (bytesSoFar < bytesNeeded) {
      try {
        int n = ins.read(buffer, bytesSoFar, bytesNeeded - bytesSoFar);
        if (n < 0) 
          return false;
        bytesSoFar += n;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }

    return true;
  }

  public void parseConnection(Socket conn) throws Exception {
    InputStream ins = conn.getInputStream();

    byte[] buffer = new byte[5000];
    if (!readToLength(ins, buffer, 0, 5))
      throw new Exception ("Unable to read header");

    if (buffer[0] != 22) 
      throw new Exception("Expected record of type 22 for handshake, but got record type "+buffer[0]);

    int recordLength = (((buffer[3] & 0xFF) << 8) | (buffer[4] & 0xFF)) + 5;
    if (!readToLength(ins, buffer, 5, recordLength))
      throw new Exception ("Unable to read header");

    byte b[] = new byte[recordLength];
    for (int i=0; i<recordLength; i++)
      b[i] = buffer[i];

    if (buffer[5] != 0x01) 
      throw new Exception("Expected client hello (type 1), but got "+buffer[5]);

    int handshakeLength = ((buffer[6] & 0xFF) << 16) | (((buffer[7] & 0xFF) << 8) | (buffer[8] & 0xFF));
    if (handshakeLength > (recordLength-4)) 
      throw new Exception("Handshake length is "+handshakeLength+", but record length is only "+recordLength);

    int pos = 11 + 32;
    int sessionIDlen = buffer[pos];
    pos += 1 + sessionIDlen;

    int cipherSuitesLen = (((buffer[pos] & 0xFF) << 8) | (buffer[pos+1] & 0xFF));
    pos += 2 + cipherSuitesLen;

    int compressionMethodslen = buffer[pos];
    pos += 1 + compressionMethodslen;

    int extensionBytesRemaining = (((buffer[pos] & 0xFF) << 8) | (buffer[pos+1] & 0xFF));
    pos += 2;

    hostName = null;
    while (extensionBytesRemaining > 0) {
      int extType = (((buffer[pos] & 0xFF) << 8) | (buffer[pos+1] & 0xFF));
      int extLen = (((buffer[pos+2] & 0xFF) << 8) | (buffer[pos+3] & 0xFF));
      pos += 4;
      if (extLen > (extensionBytesRemaining-4))
        extLen = (extensionBytesRemaining-4);

      byte b2[] = new byte[extLen];
      for (int i=0; i<extLen; i++)
        b2[i] = b[pos+i];

      if ((extType == 0) && (extLen>=2)) {
        int sniLen = (((buffer[pos] & 0xFF) << 8) | (buffer[pos+1] & 0xFF));
        if (sniLen != extLen-2)
          throw new Exception("Invalid SNI length "+sniLen);
        if (buffer[pos+2] != 0)
          throw new Exception("Invalid SNI name type "+buffer[pos+2]);
        int sniNameLen = (((buffer[pos+3] & 0xFF) << 8) | (buffer[pos+4] & 0xFF));
        if (sniNameLen != extLen-5)
          throw new Exception("Invalid SNI name length "+sniNameLen);
        byte b3[] = new byte[sniNameLen];
        for (int i=0; i<sniNameLen; i++)
          b3[i] = buffer[pos+5+i];

        hostName = new SNIHostName(b3);
      }

      pos += extLen;
      extensionBytesRemaining -= 4+extLen;
    }

    bais = new ByteArrayInputStream(buffer, 0, recordLength);
  }
}