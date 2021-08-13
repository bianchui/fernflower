package com.github.bianchui.ff.pgmapping;

public final class JavaTypes {
  public static String javaTypeToDescriptor(String type) {
    int iArray = type.lastIndexOf("[]");
    if (iArray != -1) {
      assert iArray + 2 == type.length();
      return '[' + javaTypeToDescriptor(type.substring(0, iArray));
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

  public static String typeDescriptorToJavaType(String typeDescriptor) {
    int arrayCount = 0;
    int i = 0;
    while (typeDescriptor.charAt(i) == '[') {
      ++i;
      ++arrayCount;
    }
    String type = "";
    switch (typeDescriptor.charAt(i)) {
      case 'V':
        type = "void";
        break;
      case 'C':
        type = "char";
        break;
      case 'B':
        type = "byte";
        break;
      case 'Z':
        type = "boolean";
        break;
      case 'S':
        type = "short";
        break;
      case 'I':
        type = "int";
        break;
      case 'J':
        type = "long";
        break;
      case 'F':
        type = "float";
        break;
      case 'D':
        type = "double";
        break;
      case 'L':
        type = typeDescriptor.substring(i + 1, typeDescriptor.indexOf(';', i + 1)).replace('/', '.');
        break;
    }
    if (arrayCount == 0) {
      return type;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(type);
    for (i = 0; i < arrayCount; ++i) {
      sb.append("[]");
    }
    return sb.toString();
  }

  public static String genMethodDescriptor(String ret, String args) {
    StringBuilder sb = new StringBuilder(args.length() + ret.length() + 10);
    sb.append('(');
    if (args.length() != 0) {
      int i = 0;
      while (true) {
        final int endI = args.indexOf(',', i);
        String arg = args.substring(i, endI == -1 ? args.length() : endI);
        sb.append((javaTypeToDescriptor(arg)));
        if (endI == -1) {
          break;
        }
        i = endI + 1;
      }
    }
    sb.append(')');
    sb.append(javaTypeToDescriptor(ret));
    return sb.toString();
  }

  public static int getDescriptorEnd(String descriptor, int start) {
    int i = start;
    while (descriptor.charAt(i) == '[') {
      ++i;
    }
    if (descriptor.charAt(i) == 'L') {
      i = descriptor.indexOf(';', i + 1) + 1;
    } else {
      ++i;
    }
    return i;
  }
}
