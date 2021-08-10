package com.github.bianchui.ff.utils;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;

public class MyLogger {
  public static StructMethod currentMethod;
  public static void error(String format, Object ... args) {
    StructClass currentClass = (StructClass)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS);
    String s = String.format(format, args);
    System.err.printf("!!ERROR!!: %s %s %s : %s\n", currentClass.qualifiedName, currentMethod.getName(), currentMethod.getDescriptor(), s);
  }
}
