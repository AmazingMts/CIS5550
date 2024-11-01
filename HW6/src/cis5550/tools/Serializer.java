package cis5550.tools;

import java.util.*;
import java.net.*;
import java.io.*;

public class Serializer {
  public static byte[] objectToByteArray(Object o) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(o);
      oos.flush();
      return baos.toByteArray();
    } catch (Exception e) {
     	e.printStackTrace();
    }
    return null;
  }

  public static Object byteArrayToObject(byte b[], File jarFileToLoadClassesFrom) {
  	Object result = null;
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(b);
      ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
      URLClassLoader newCL = (jarFileToLoadClassesFrom != null) ? new URLClassLoader (new URL[] {jarFileToLoadClassesFrom.toURI().toURL()}, oldCL) : null;
      ObjectInputStream ois = new ObjectInputStream(bais) {
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          try {
            Class<?> x = Class.forName(desc.getName(), false, null);
            return x;
          } catch (ClassNotFoundException cnfe) {
            if (newCL != null) 
              return newCL.loadClass(desc.getName());
          }
          return null;
        }
      };
      result = ois.readObject();
    } catch (Exception e) {
     	e.printStackTrace();
    }
    return result;
  }
}
