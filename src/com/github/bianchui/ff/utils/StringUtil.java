// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.utils;

public final class StringUtil {
  public static int countSp(String name, int sp) {
    int count = 0;
    int i = 0;
    while ((i = (name.indexOf(sp, i) + 1)) != 0) {
      ++count;
    }
    return count;
  }

}
