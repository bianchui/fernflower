// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
