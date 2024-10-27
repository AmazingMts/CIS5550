package cis5550.tools;

import cis5550.flame.FlameContext;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class Loader {
  public static void invokeRunMethod(File jarFile, String className, FlameContext arg1, Vector<String> arg2) throws IllegalAccessException, InvocationTargetException, MalformedURLException, ClassNotFoundException, NoSuchMethodException {
    URLClassLoader cl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, ClassLoader.getSystemClassLoader());
    Class<?> classToLoad = Class.forName(className, true, cl);
    Method method = classToLoad.getMethod("run", FlameContext.class, String[].class);
    method.invoke(null, new Object[] { arg1, arg2.toArray(new String[] {}) });
  }
}