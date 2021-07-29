package com.github.bianchui.pgmapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingReader {
  private static class FieldInfo {
    String _fieldOrg; // java.lang.String TAG
    String _orgType;
    String _orgName;
    String _mapName;
  }
  private static class MethodInfo {
    String _methodOrg; // java.lang.String[] method(java.lang.String,int)
    String _orgSig;
    String _orgName;
    String _mapSig; // gen in second pass
    String _mapName;
  }
  private static class ClassInfo {
    String _orgName;
    String _mapName;
    List<FieldInfo> _fields;
    Map<String, FieldInfo> _mapFields; // gen in second pass
    List<MethodInfo> _methods;
    Map<String, MethodInfo> _mapMethods; // gen in second pass
  }
  private final Map<String, ClassInfo> _orgNameClasses = new HashMap<>();
  private final Map<String, ClassInfo> _mapNameClasses = new HashMap<>();

  public MappingReader(File file) {
    readFile(file);
  }

  private void readFile(File file) {
    FileReader fileReader = null;
    BufferedReader bufferedReader = null;
    final String kNameSp = " -> ";
    try {
      fileReader = new FileReader(file);
      bufferedReader = new BufferedReader(fileReader, 2048);
      String line;
      ClassInfo cls = null;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#")) {
          continue;
        }
        if (line.startsWith(" ")) {
          // continue class
          if (cls == null) {
            continue;
          }
          line = line.trim();
          if (line.length() == 0) {
            continue;
          }
          final int nameSp = line.indexOf(kNameSp);
          if (nameSp == -1) {
            continue;
          }
          final int startSp = line.lastIndexOf(':', nameSp);
          if (startSp != -1) {
            // method
            final String methodOrg = line.substring(startSp + 1, nameSp).trim();
            final String mapName = line.substring(nameSp + kNameSp.length()).trim();
            final int iNameEnd = methodOrg.indexOf('(');
            final int iParamEnd = methodOrg.indexOf(')', iNameEnd + 1);
            if (iNameEnd == -1 || iParamEnd ==  -1) {
              continue;
            }
            final int iSpace = methodOrg.lastIndexOf(' ', iNameEnd);
            if (iSpace == -1) {
              continue;
            }
            final MethodInfo methodInfo = new MethodInfo();
            methodInfo._mapName = mapName;
            methodInfo._methodOrg = methodOrg;
            methodInfo._orgName = methodOrg.substring(iSpace + 1, iNameEnd);
            final String ret = methodOrg.substring(0, iSpace);
            final String args = methodOrg.substring(iNameEnd + 1, iParamEnd);
            methodInfo._orgSig = JavaTypes.genMethodSignature(ret, args);
            cls._methods.add(methodInfo);
          } else {
            // field

            cls._fields.add(new FieldInfo());
          }
        } else {
          // new class
          final int nameSp = line.indexOf(kNameSp);
          final int endSp = line.indexOf(':', nameSp + 1);
          if (nameSp != -1 && endSp != -1) {
            final String orgName = line.substring(0, nameSp).trim();
            final String mapName = line.substring(nameSp + kNameSp.length(), endSp).trim();
            if (orgName.length() > 0 && mapName.length() > 0) {
              cls = new ClassInfo();
              cls._orgName = orgName;
              cls._mapName = mapName;
              _orgNameClasses.put(orgName, cls);
              _mapNameClasses.put(mapName, cls);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fileReader != null) {
        try {
          fileReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
