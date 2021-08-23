// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.pgmapping;

import com.github.bianchui.ff.utils.JavaTypes;
import com.github.bianchui.ff.utils.StringUtil;

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
    String _fieldOrg; // "java/lang/String TAG"
    String _orgType; // "java/lang/String"
    String _orgName; // "TAG"
    String _mapType; // "java/lang/String" gen in processMapping
    String _mapDesc; // "Ljava/lang/String;" gen in processMapping
    String _mapName; // "a"

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
    String _methodOrg; // "java/lang/String[] method(java/lang/String,int)"
    String _orgRetType; // "java/lang/String[]"
    String _orgArgs; // "java/lang/String,int"
    String _orgDesc; // "Ljava/lang/String;I"
    String _orgName; // "method"
    String _mapDesc; // "(Ljava/lang/String;I)[Ljava/lang/String;" gen in processMapping
    String _mapName; // "a"

    String orgFunction() {
      return String.format("%s %s(%s); // %s", _orgRetType, _orgName, _orgArgs, _orgDesc);
    }
    String mapFunction() {
      return String.format("%s %s", _mapName, _mapDesc);
    }
    String mapKey() {
      return _mapName + " " + _mapDesc;
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
  private final Map<String, String> _mapPackages = new HashMap<>();

  public MappingReader(File file) {
    readFile(file);
    processMapping();
  }

  private void mapPackageForClass(String orgClsName, String mapClsName) {
    final int classSp = mapClsName.lastIndexOf('/');
    if (classSp <= 0 || StringUtil.countSp(orgClsName, '/') != StringUtil.countSp(mapClsName, '/')) {
      return;
    }
    if (_mapPackages.containsKey(mapClsName.substring(0, classSp))) {
      return;
    }
    int iMap = 0, iOrg = 0;
    while ((iMap = (mapClsName.indexOf('/', iMap) + 1)) != 0) {
      iOrg = orgClsName.indexOf('/', iOrg) + 1;
      final String mapPkg = mapClsName.substring(0, iMap - 1);
      final String orgPkg = orgClsName.substring(0, iOrg - 1);
      if (!orgPkg.equals(mapPkg) && !_mapPackages.containsKey(mapPkg)) {
        _mapPackages.put(mapPkg, orgPkg);
      }
    }
  }

  public String getOrgPackage(String mapPkg) {
    return _mapPackages.get(mapPkg);
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
        // replace com.github.bianchui -> com/github/bianchui
        line = line.replace('.', '/');
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
          final boolean isMethod = line.indexOf('(') >= 0;
          final int startSp = line.lastIndexOf(':', nameSp);
          final String orgStr = line.substring(startSp + 1, nameSp).trim();
          final String mapName = line.substring(nameSp + kNameSp.length()).trim();
          if (isMethod) {
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
            methodInfo._orgDesc = JavaTypes.genMethodDescriptor(ret, args);
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
              mapPackageForClass(orgName, mapName);
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
    fieldInfo._mapDesc = JavaTypes.javaTypeToDescriptor(fieldInfo._mapType);
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
    methodInfo._mapDesc = JavaTypes.genMethodDescriptor(mapRetType, mapArgs);
  }

  public void dump() {
    for (ClassInfo classInfo : _classes) {
      System.out.printf("// %s\n", classInfo._orgName);
      System.out.printf("class %s {\n", classInfo._mapName);
      if (classInfo._fields != null) {
        for (FieldInfo info : classInfo._fields) {
          System.out.printf("  // %s\n", info.orgField());
          System.out.printf("  %s\n", info.mapField());
        }
      }
      if (classInfo._methods != null) {
        for (MethodInfo info : classInfo._methods) {
          System.out.printf("  // %s\n", info.orgFunction());
          System.out.printf("  %s\n", info.mapFunction());
        }
      }
      System.out.printf("}\n");
    }
  }

  public String getClassOrgName(String className) {
    ClassInfo classInfo = _mapNameClasses.get(className);
    return classInfo != null ? classInfo._orgName : null;
  }

  public String getClassMapName(String orgClassName) {
    ClassInfo classInfo = _orgNameClasses.get(orgClassName);
    return classInfo != null ? classInfo._mapName : null;
  }

  public String getFieldOrgName(String className, String field, String descriptor) {
    ClassInfo classInfo = _mapNameClasses.get(className);
    if (classInfo != null && classInfo._mapFields != null) {
      FieldInfo fieldInfo = classInfo._mapFields.get(field + " " + descriptor);
      return fieldInfo != null ? fieldInfo._orgName : null;
    }
    return null;
  }

  public String getMethodOrgName(String className, String method, String descriptor) {
    ClassInfo classInfo = _mapNameClasses.get(className);
    if (classInfo != null && classInfo._mapMethods != null) {
      MethodInfo methodInfo = classInfo._mapMethods.get(method + " " + descriptor);
      return methodInfo != null ? methodInfo._orgName : null;
    }
    return null;
  }
}
