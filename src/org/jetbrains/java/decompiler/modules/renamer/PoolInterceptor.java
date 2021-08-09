// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import java.util.HashMap;
import java.util.Map;

public class PoolInterceptor {
  private final Map<String, String> mapOldToNewNames = new HashMap<>();
  private final Map<String, String> mapNewToOldNames = new HashMap<>();

  public void addName(String oldName, String newName) {
    mapOldToNewNames.put(oldName, newName);
    mapNewToOldNames.put(newName, oldName);
  }

  public String getName(String oldName) {
    return mapOldToNewNames.get(oldName);
    String ret = mapOldToNewNames.get(oldName);
    if (ret == null) {
      String[] strings = oldName.split(" ");
      if (strings.length == 3) {
        strings[0] = getOldName(strings[0]);
        if (strings[0] != null) {
          oldName = strings[0] + " " + strings[1] + " " + strings[2];
          ret = mapOldToNewNames.get(oldName);
        }
      }
    }
    return ret;
  }

  public String getOldName(String newName) {
    return mapNewToOldNames.get(newName);
  }
}