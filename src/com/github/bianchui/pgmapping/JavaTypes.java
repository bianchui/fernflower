package com.github.bianchui.pgmapping;

public final class JavaTypes {
  public static String mapJavaTypeToSignature(String type) {
    int iArray = type.indexOf("[]");
    if (iArray != -1) {

    }
    if (type.startsWith("["))
      return type.replace(".", "/");
    if (type.equals("int"))
      return "I";
    if (type.equals("float"))
      return "F";
    if (type.equals("long"))
      return "J";
    if (type.equals("double"))
      return "D";
    if (type.equals("short"))
      return "S";
    if (type.equals("char"))
      return "C";
    if (type.equals("boolean"))
      return "Z";
    if (type.equals("byte"))
      return "B";
    return ("L" + type + ";").replace(".", "/");
  }

  public static String genMethodSignature(String ret, String args) {
    return ret;
  }
}
