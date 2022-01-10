// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.utils;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;

import java.util.Locale;

public class RenamerUtil {
  public static String getClassPackage(String clsFullName) {
    int index = clsFullName.lastIndexOf('/');
    return index == -1 ? "" : clsFullName.substring(0, index);
  }

  public static String concatPackage(String parent, String subName) {
    return parent == null || parent.length() == 0 ? subName : parent + '/' + subName;
  }

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

  public static String typeDescriptorShortName(String typeDescriptor, int startIndex) {
    StringBuilder sb = new StringBuilder();
    int i = startIndex;
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
        final int nameInner = name.lastIndexOf('$') + 1;
        if (nameInner > nameStart && nameInner != name.length() - 1) {
          nameStart = nameInner;
        }
        sb.append(name.substring(nameStart, nameStart + 1).toLowerCase(Locale.US));
        sb.append(name.substring(nameStart + 1));
        break;
    }
    return sb.toString();
  }

  public static void typeDescriptorVeryShortName(String typeDescriptor, int startIndex, StringBuilder sb) {
    int i = startIndex;
    while (typeDescriptor.charAt(i) == '[') {
      sb.append("A");
      ++i;
    }
    switch (typeDescriptor.charAt(i)) {
      case 'V':
        sb.append("V");
        break;
      case 'C':
        sb.append("C");
        break;
      case 'B':
        sb.append("B");
        break;
      case 'Z':
        sb.append("Z");
        break;
      case 'S':
        sb.append("S");
        break;
      case 'I':
        sb.append("I");
        break;
      case 'J':
        sb.append("J");
        break;
      case 'F':
        sb.append("F");
        break;
      case 'D':
        sb.append("D");
        break;
      case 'L':
        String name = getRenamedClassName(typeDescriptor.substring(i + 1, typeDescriptor.indexOf(';', i + 1)));
        int nameStart = name.lastIndexOf('/') + 1;
        String nameShort = name.substring(nameStart);
        if (nameShort.startsWith("Class_") && nameShort.length() != 6) {
          nameShort = nameShort.substring(6);
        }
        sb.append(nameShort.substring(0, 1).toUpperCase(Locale.US));
        sb.append(nameShort.substring(1));
        break;
    }
  }

  public static String methodDescriptorVeryShortName(String methodDescriptor) {
    StringBuilder sb = new StringBuilder();
    if (methodDescriptor.charAt(0) == '(') {
      int count = 0;
      int i = 1;
      while (methodDescriptor.charAt(i) != ')') {
        typeDescriptorVeryShortName(methodDescriptor, i, sb);
        i = JavaTypes.getDescriptorEnd(methodDescriptor, i);
        ++count;
      }
    }
    return sb.toString();
  }
}
