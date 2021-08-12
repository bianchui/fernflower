package com.github.bianchui.ff.utils;

public class ClassUtil {
  public static String getClassShortName(String fullName) {
    return fullName.substring(fullName.lastIndexOf('/') + 1);
  }

  public static String getClassInnerName(String fullName) {
    final int innerIndex = fullName.lastIndexOf('$');
    if (innerIndex != -1 && fullName.indexOf('/', innerIndex + 1) == -1) {
      return fullName.substring(innerIndex + 1);
    }
    return null;
  }
}
