package com.github.bianchui.ff.utils;

// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;

public class MyLogger {
  public static StructMethod currentMethod;
  private static final boolean DEBUG = false;

  public static final boolean CAT_RENAME = false;
  public static final boolean CAT_GUESS_NAME = false;
  public static final boolean CAT_PROCESS = false;
  public static final boolean CAT_GENERIC = false;
  public static final boolean DumpClassTree = false;

  public static void log(String format, Object ... args) {
    if (!DEBUG) {
      return;
    }
    System.out.printf(format, args);
  }

  public static void log(boolean log, String format, Object ... args) {
    if (!DEBUG && !log) {
      return;
    }
    System.out.printf(format, args);
  }

  public static void error(boolean log, String format, Object ... args) {
    System.err.print("!!ERROR!!: ");
    System.err.printf(format, args);
    System.err.flush();
  }

  public static void guess_name_log(String format, Object ... args) {
    log(CAT_GUESS_NAME, format, args);
  }
  public static void guess_name_error(String format, Object ... args) {
    error(CAT_GUESS_NAME, format, args);
  }

  public static void process_log(String format, Object ... args) {
    log(CAT_PROCESS, format, args);
  }

  public static void rename_log(String format, Object ... args) {
    log(CAT_RENAME, format, args);
  }
  public static void generic_log(String format, Object ... args) {
    log(CAT_GENERIC, format, args);
  }

  public static void decompile_error(String format, Object ... args) {
    StructClass currentClass = (StructClass)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS);
    String s = String.format(format, args);
    System.err.printf("!!ERROR!!: %s %s %s : %s\n", currentClass.qualifiedName, currentMethod.getName(), currentMethod.getDescriptor(), s);
  }
}
