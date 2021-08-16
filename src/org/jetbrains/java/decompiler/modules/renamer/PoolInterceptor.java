// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import com.github.bianchui.ff.utils.MyLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoolInterceptor {
  private final Map<String, String> mapOldToNewNames = new HashMap<>();
  private final Map<String, String> mapNewToOldNames = new HashMap<>();
  private static class PkgNameMaps {
    final String oldName;
    final String newName;
    PkgNameMaps(String oldName, String newName) {
      this.oldName = oldName;
      this.newName = newName;
    }
  }
  private final List<PkgNameMaps> mapOldToNewPkgs = new ArrayList<>();

  public void redirectPackage(String oldPkg, String newPkg) {
    mapOldToNewPkgs.add(new PkgNameMaps(oldPkg, newPkg));
  }

  public void renameClass(String oldName, String newName) {
    mapOldToNewPkgs.add(new PkgNameMaps(oldName, newName));
  }

  public void addName(String oldName, String newName) {
    mapOldToNewNames.put(oldName, newName);
    mapNewToOldNames.put(newName, oldName);
  }

  public String getName(String oldName) {
    final boolean isClass = oldName.indexOf(' ') == -1;
    if (mapOldToNewPkgs.size() > 0 && isClass) {
      for (PkgNameMaps entry : mapOldToNewPkgs) {
        if (oldName.startsWith(entry.oldName)) {
          return entry.newName + oldName.substring(entry.oldName.length());
        }
      }
    }
    if (mapOldToNewNames.isEmpty()) {
      return null;
    }
    String ret = mapOldToNewNames.get(oldName);
    if (!mapOldToNewNames.isEmpty()) {
      MyLogger.log("Interceptor.getName(%s) -> %s\n", oldName, ret);
    }
    if (ret == null && !isClass) {
      String[] strings = oldName.split(" ");
      if (strings.length == 3) {
        strings[0] = getOldName(strings[0]);
        if (strings[0] != null) {
          ret = mapOldToNewNames.get(strings[0] + " " + strings[1] + " " + strings[2]);
          if (!mapOldToNewNames.isEmpty()) {
            MyLogger.log("  Interceptor.getName(%s) => %s\n", oldName, ret);
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