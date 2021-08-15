// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import com.github.bianchui.ff.utils.MyLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoolInterceptor {
  private final Map<String, String> mapOldToNewNames = new HashMap<>();
  private final Map<String, String> mapNewToOldNames = new HashMap<>();
  private final Map<String, String> mapOldToNewPkgs = new HashMap<>();
  private final Map<String, String> mapNewToOldPkgs = new HashMap<>();

  public void redirectPackage(String oldPkg, String newPkg) {
    mapOldToNewPkgs.put(oldPkg, newPkg);
    mapNewToOldPkgs.put(newPkg, oldPkg);
  }

  public void addName(String oldName, String newName) {
    mapOldToNewNames.put(oldName, newName);
    mapNewToOldNames.put(newName, oldName);
  }

  public String getName(String oldName) {
    for (Map.Entry<String, String> entry : mapOldToNewPkgs.entrySet()) {
      if (oldName.startsWith(entry.getKey())) {
        return entry.getValue() + oldName.substring(entry.getKey().length());
      }
    }
    String ret = mapOldToNewNames.get(oldName);
    if (!mapOldToNewNames.isEmpty()) {
      MyLogger.log("Interceptor.getName(%s) -> %s\n", oldName, ret);
    }
    if (ret == null) {
      String[] strings = oldName.split(" ");
      if (strings.length == 3) {
        strings[0] = getOldName(strings[0]);
        if (strings[0] != null) {
          oldName = strings[0] + " " + strings[1] + " " + strings[2];
          ret = mapOldToNewNames.get(oldName);
          if (!mapOldToNewNames.isEmpty()) {
            MyLogger.log("  Interceptor.getName(%s) -> %s\n", oldName, ret);
          }
        }
      }
    }
    return ret;
  }

  public String getOldName(String newName) {
    return mapNewToOldNames.get(newName);
  }
}