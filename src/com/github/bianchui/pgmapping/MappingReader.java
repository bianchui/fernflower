package com.github.bianchui.pgmapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingReader {
  private static class FieldInfo {
    String _fieldOrg; // java.lang.String TAG
    String _orgType;
    String _orgName;
    String _mapType; // gen in processMapping
    String _mapDesc; // gen in processMapping
    String _mapName;

    String orgField() {
      return String.format("%s %s;", _orgType, _orgName);
    }
    String mapField() {
      return String.format("%s %s; // %s", _mapType, _mapName, _mapDesc);
    }
    String mapKey() {
      return _mapName + " " + _mapDesc;
    }
  }
  private static class MethodInfo {
    String _methodOrg; // java.lang.String[] method(java.lang.String,int)
    String _orgRetType;
    String _orgArgs;
    String _orgSig;
    String _orgName;
    String _mapSig; // gen in processMapping
    String _mapName;

    String orgFunction() {
      return String.format("%s %s(%s); // %s", _orgRetType, _orgName, _orgArgs, _orgSig);
    }
    String mapFunction() {
      return String.format("%s %s", _mapName, _mapSig);
    }
    String mapKey() {
      return _mapName + " " + _mapSig;
    }
  }
  private static class ClassInfo {
    String _orgName;
    String _mapName;
    List<FieldInfo> _fields;
    Map<String, FieldInfo> _mapFields; // gen in processMapping
    List<MethodInfo> _methods;
    Map<String, MethodInfo> _mapMethods; // gen in processMapping
  }
  private final List<ClassInfo> _classes = new ArrayList<>();
  private final Map<String, ClassInfo> _orgNameClasses = new HashMap<>();
  private final Map<String, ClassInfo> _mapNameClasses = new HashMap<>();

  public MappingReader(File file) {
    readFile(file);
    processMapping();
  }

  private void readFile(File file) {
    FileReader fileReader = null;
    BufferedReader bufferedReader = null;
    assert false;
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
          final String orgStr = line.substring(startSp + 1, nameSp).trim();
          final String mapName = line.substring(nameSp + kNameSp.length()).trim();
          if (startSp != -1) {
            // method
            final int iNameEnd = orgStr.indexOf('(');
            final int iParamEnd = orgStr.indexOf(')', iNameEnd + 1);
            if (iNameEnd == -1 || iParamEnd ==  -1) {
              assert false;
              continue;
            }
            final int iSpace = orgStr.lastIndexOf(' ', iNameEnd);
            if (iSpace == -1) {
              assert false;
              continue;
            }
            final MethodInfo methodInfo = new MethodInfo();
            methodInfo._mapName = mapName;
            methodInfo._methodOrg = orgStr;
            methodInfo._orgName = orgStr.substring(iSpace + 1, iNameEnd);
            if (methodInfo._orgName.indexOf('.') != -1) {
              continue;
            }
            final String ret = orgStr.substring(0, iSpace);
            final String args = orgStr.substring(iNameEnd + 1, iParamEnd);
            methodInfo._orgRetType = ret;
            methodInfo._orgArgs = args;
            methodInfo._orgSig = JavaTypes.genMethodSignature(ret, args);
            if (cls._methods == null) {
              cls._methods = new ArrayList<>();
            }
            cls._methods.add(methodInfo);
          } else {
            // field
            final int iSpace = orgStr.lastIndexOf(' ', nameSp);
            if (iSpace == -1) {
              continue;
            }

            final FieldInfo fieldInfo = new FieldInfo();
            fieldInfo._fieldOrg = orgStr;
            fieldInfo._mapName = mapName;
            fieldInfo._orgName = orgStr.substring(iSpace + 1, nameSp);
            if (fieldInfo._orgName.indexOf('.') != -1) {
              continue;
            }
            fieldInfo._orgType = orgStr.substring(0, iSpace);
            if (cls._fields == null) {
              cls._fields = new ArrayList<>();
            }
            cls._fields.add(fieldInfo);
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
              _classes.add(cls);
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

  private void processMapping() {
    for (ClassInfo classInfo : _classes) {
      if (classInfo._fields != null) {
        classInfo._mapFields = new HashMap<>();
        for (FieldInfo fieldInfo : classInfo._fields) {
          mapField(fieldInfo);
          classInfo._mapFields.put(fieldInfo.mapKey(), fieldInfo);
        }
      }

      if (classInfo._methods != null) {
        classInfo._mapMethods = new HashMap<>();
        for (MethodInfo methodInfo : classInfo._methods) {
          mapMethod(methodInfo);
          classInfo._mapMethods.put(methodInfo.mapKey(), methodInfo);
        }
      }
    }
  }

  private String mapType(String orgType) {
    int firstArray = orgType.indexOf("[]");
    if (firstArray != -1) {
      return mapType(orgType.substring(0, firstArray)) + orgType.substring(firstArray);
    }
    ClassInfo classInfo = _orgNameClasses.get(orgType);
    if (classInfo == null) {
      return orgType;
    }
    return classInfo._mapName;
  }

  private void mapField(FieldInfo fieldInfo) {
    fieldInfo._mapType = mapType(fieldInfo._orgType);
    fieldInfo._mapDesc = JavaTypes.mapJavaTypeToSignature(fieldInfo._mapType);
  }

  private void mapMethod(MethodInfo methodInfo) {
    String mapRetType = mapType(methodInfo._orgRetType);
    int i = 0;
    String args = methodInfo._orgArgs;
    StringBuilder sb = new StringBuilder();
    while (true) {
      int endI = args.indexOf(',', i);
      String arg = args.substring(i, endI == -1 ? args.length() : endI);
      sb.append(mapType(arg));
      if (endI == -1) {
        break;
      }
      sb.append(',');
      i = endI + 1;
    }
    String mapArgs = sb.toString();
    methodInfo._mapSig = JavaTypes.genMethodSignature(mapRetType, mapArgs);
  }

  public void dump() {
    for (ClassInfo classInfo : _classes) {
      System.out.printf("class %s {\n", classInfo._orgName);
      if (classInfo._fields != null) {
        for (FieldInfo info : classInfo._fields) {
          System.out.printf("  %s // -> %s\n", info.orgField(), info.mapField());
        }
      }
      if (classInfo._methods != null) {
        for (MethodInfo info : classInfo._methods) {
          System.out.printf("  %s // -> %s\n", info.orgFunction(), info.mapFunction());
        }
      }
      System.out.printf("}\n");
    }
  }
}
