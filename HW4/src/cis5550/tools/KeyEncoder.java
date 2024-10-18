package cis5550.tools;

public class KeyEncoder {

  public static String encode(String key) {
    String allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-";
    String out = "";
    for (int i=0; i<key.length(); i++) {
    	if (allowedChars.contains(""+key.charAt(i)))
    		out = out + key.charAt(i);
      else
      	out = out + "_" + Integer.toHexString((int)key.charAt(i));
    }
    return out;
  }

  public static String decode(String str) {
  	String out = "";
  	for (int i=0; i<str.length(); i++) {
  		if (str.charAt(i) == '_') {
  			out = out + (char)(int)Integer.decode("0x"+str.substring(i+1, i+3));
  			i += 2;
  		} else {
  			out = out + str.charAt(i);
  		}
  	}
    return out;
  }

  public static void main(String args[]) {
  	System.out.println(encode(args[0]));
  	System.out.println(decode(encode(args[0])));
  }
  
}