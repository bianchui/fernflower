package com.github.bianchui.ff.utils;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;

import java.util.Locale;

public class RenamerUtil {
  /**
   * class rename have 2 pass
   * first pass in renameAllClasses()
   * second pass after rename() in loadClasses() to rename inner classes
   * @param shortName
   * @param fullName
   * @return is the first pass
   */
  public static boolean isRenameAllPass(String shortName, String fullName) {
    if (!fullName.endsWith(shortName)) {
      return false;
    }
    if (fullName.length() == shortName.length()) {
      return true;
    }
    return fullName.charAt(fullName.length() - shortName.length() - 1) == '/';
  }

  public static String getRenamedClassName(String name) {
    PoolInterceptor interceptor = DecompilerContext.getPoolInterceptor();
    String newName = interceptor.getName(name);
    return newName != null ? newName : name;
  }

  public static String typeDescriptorShortName(String typeDescriptor) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (typeDescriptor.charAt(i) == '[') {
      sb.append("arr_");
      ++i;
    }
    switch (typeDescriptor.charAt(i)) {
      case 'V':
        sb.append("void");
        break;
      case 'C':
        sb.append("char");
        break;
      case 'B':
        sb.append("byte");
        break;
      case 'Z':
        sb.append("bool");
        break;
      case 'S':
        sb.append("short");
        break;
      case 'I':
        sb.append("int");
        break;
      case 'J':
        sb.append("long");
        break;
      case 'F':
        sb.append("float");
        break;
      case 'D':
        sb.append("double");
        break;
      case 'L':
        String name = getRenamedClassName(typeDescriptor.substring(i + 1, typeDescriptor.indexOf(';', i + 1)));
        int nameStart = name.lastIndexOf('/') + 1;
        sb.append(name.substring(nameStart, nameStart + 1).toLowerCase(Locale.US));
        sb.append(name.substring(nameStart + 1));
        break;
    }
    return sb.toString();
  }

  public static int getMethodArgCount(String methodDescriptor) {
    // (II)V
    if (methodDescriptor.charAt(0) != '(') {
      return 0;
    }
    int count = 0;
    int i = 1;
    while (true) {
      while (methodDescriptor.charAt(i) == '[') {
        ++i;
      }
      if (methodDescriptor.charAt(i) == ')') {
        break;
      }
      if (methodDescriptor.charAt(i) == 'L') {
        i = methodDescriptor.indexOf(';', i + 1) + 1;
        if (i == 0) {
          break;
        }
      } else {
        ++i;
      }
      ++count;
    }
    return count;
  }
}
