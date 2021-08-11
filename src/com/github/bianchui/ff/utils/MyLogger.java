package com.github.bianchui.ff.utils;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;

public class MyLogger {
  public static StructMethod currentMethod;
  private static final boolean DEBUG = false;

  public static final int CAT_RENAME = 1;

  public static void log(String format, Object ... args) {
    if (!DEBUG) {
      return;
    }
    System.out.printf(format, args);
  }

  public static void log(int cat, String format, Object ... args) {
    if (!DEBUG) {
      return;
    }
    System.out.printf(format, args);
  }

  public static void rename_log(String format, Object ... args) {
    log(CAT_RENAME, format, args);
  }

  public static void decompile_error(String format, Object ... args) {
    StructClass currentClass = (StructClass)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS);
    String s = String.format(format, args);
    System.err.printf("!!ERROR!!: %s %s %s : %s\n", currentClass.qualifiedName, currentMethod.getName(), currentMethod.getDescriptor(), s);
  }
}
