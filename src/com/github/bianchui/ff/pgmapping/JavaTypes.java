package com.github.bianchui.ff.pgmapping;

public final class JavaTypes {
  public static String mapJavaTypeToSignature(String type) {
    int iArray = type.lastIndexOf("[]");
    if (iArray != -1) {
      assert iArray + 2 == type.length();
      return '[' + mapJavaTypeToSignature(type.substring(0, iArray));
    }
    if (type.equals("void")) {
      return "V";
    }
    if (type.equals("int")) {
      return "I";
    }
    if (type.equals("float")) {
      return "F";
    }
    if (type.equals("long")) {
      return "J";
    }
    if (type.equals("double")) {
      return "D";
    }
    if (type.equals("short")) {
      return "S";
    }
    if (type.equals("char")) {
      return "C";
    }
    if (type.equals("boolean")) {
      return "Z";
    }
    if (type.equals("byte")) {
      return "B";
    }
    return ("L" + type + ";").replace(".", "/");
  }

  public static String genMethodSignature(String ret, String args) {
    StringBuilder sb = new StringBuilder(args.length() + ret.length() + 10);
    sb.append('(');
    if (args.length() != 0) {
      int i = 0;
      while (true) {
        final int endI = args.indexOf(',', i);
        String arg = args.substring(i, endI == -1 ? args.length() : endI);
        sb.append((mapJavaTypeToSignature(arg)));
        if (endI == -1) {
          break;
        }
        i = endI + 1;
      }
    }
    sb.append(')');
    sb.append(mapJavaTypeToSignature(ret));
    return sb.toString();
  }
}
