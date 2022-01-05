// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.pgmapping;

import com.github.bianchui.ff.utils.JavaTypes;
import com.github.bianchui.ff.utils.NaturalOrderStringComparator;
import com.github.bianchui.ff.utils.RenamerUtil;

import java.util.*;

public class MappingGen {
  private static class FieldInfo {
    String _orgName; // "TAG"
    String _mapDesc; // "Ljava/lang/String;"
    String _mapName; // "a"
  }
  private static class MethodInfo {
    String _orgName; // "method"
    String _mapDesc; // "(Ljava/lang/String;I)[Ljava/lang/String;"
    String _mapName; // "a"
  }
  private static class ClassInfo {
    String _orgName;
    String _mapName;
    List<FieldInfo> _fields;
    List<MethodInfo> _methods;
  }

  private final Map<String, ClassInfo> _mapNameClasses = new HashMap<>();
  private final Map<String, String> _mapPackages = new HashMap<>();

  public void addPackageMap(String org, String map) {
    _mapPackages.put(map, org);
  }

  // map name should get org packageName then add new
  public void addMapClass(String orgShortName, String mapFullName) {
    ClassInfo classInfo = _mapNameClasses.get(mapFullName);
    if (classInfo != null) {
      throw new RuntimeException("Class " + mapFullName + " exist.");
    }
    final String mapPkg = RenamerUtil.getClassPackage(mapFullName);
    final String orgPkg = mapPkg.isEmpty() ? mapPkg : _mapPackages.get(mapPkg);
    if (orgPkg == null) {
      throw new RuntimeException("Package " + mapPkg + " not found, for " + orgShortName + ", " + mapFullName + ".");
    }
    classInfo = new ClassInfo();
    classInfo._orgName = orgPkg + "/" + orgShortName;
    classInfo._mapName = mapFullName;
    _mapNameClasses.put(mapFullName, classInfo);
  }

  public void addMapField(String mapFullClassName, String orgName, String mapName, String mapDescriptor) {
    ClassInfo classInfo = _mapNameClasses.get(mapFullClassName);
    if (classInfo == null) {
      throw new RuntimeException("Class " + mapFullClassName + " not found.");
    }
    FieldInfo fieldInfo = new FieldInfo();
    fieldInfo._mapDesc = mapDescriptor;
    fieldInfo._mapName = mapName;
    fieldInfo._orgName = orgName;
    if (classInfo._fields == null) {
      classInfo._fields = new ArrayList<>();
    }
    classInfo._fields.add(fieldInfo);
  }

  public void addMapMethod(String mapFullClassName, String orgName, String mapName, String mapDescriptor) {
    if (orgName.startsWith("<")) {
      return;
    }
    ClassInfo classInfo = _mapNameClasses.get(mapFullClassName);
    if (classInfo == null) {
      throw new RuntimeException("Class " + mapFullClassName + " not found.");
    }
    MethodInfo methodInfo = new MethodInfo();
    methodInfo._mapDesc = mapDescriptor;
    methodInfo._mapName = mapName;
    methodInfo._orgName = orgName;
    if (classInfo._methods == null) {
      classInfo._methods = new ArrayList<>();
    }
    classInfo._methods.add(methodInfo);
  }

  private String getOrgTypeDescriptor(String mapDescriptor) {
    final int lastArray = mapDescriptor.lastIndexOf('[');
    if (lastArray != -1) {
      return mapDescriptor.substring(0, lastArray + 1) + getOrgTypeDescriptor(mapDescriptor.substring(lastArray + 1));
    }
    if (mapDescriptor.charAt(0) != 'L') {
      return mapDescriptor;
    }
    ClassInfo orgClass = _mapNameClasses.get(mapDescriptor.substring(1, mapDescriptor.length() - 1));
    if (orgClass == null) {
      return mapDescriptor;
    }
    return "L" + orgClass._orgName + ";";
  }

  private String getOrgMethodDescriptor(String methodDescriptor) {
    if (methodDescriptor.charAt(0) != '(') {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    int i = 1;
    while (methodDescriptor.charAt(i) != ')') {
      final int end = JavaTypes.getDescriptorEnd(methodDescriptor, i);
      sb.append(getOrgTypeDescriptor(methodDescriptor.substring(i, end)));
      i = end;
    }
    sb.append(')');
    sb.append(getOrgTypeDescriptor(methodDescriptor.substring(i + 1)));
    return sb.toString();
  }

  private String getOrgJavaMethod(String methodDescriptor, String methodName) {
    if (methodDescriptor.charAt(0) != '(') {
      return null;
    }
    final int argsEnd = methodDescriptor.indexOf(')', 1);
    StringBuilder sb = new StringBuilder();
    sb.append(JavaTypes.typeDescriptorToJavaType(getOrgTypeDescriptor(methodDescriptor.substring(argsEnd + 1))));
    sb.append(' ');
    sb.append(methodName);
    sb.append('(');
    int i = 1;
    while (i < argsEnd) {
      if (i != 1) {
        sb.append(',');
      }
      final int end = JavaTypes.getDescriptorEnd(methodDescriptor, i);
      sb.append(JavaTypes.typeDescriptorToJavaType(getOrgTypeDescriptor(methodDescriptor.substring(i, end))));
      i = end;
    }
    sb.append(')');
    return sb.toString();
  }


  public String genMap() {
    StringBuilder sb = new StringBuilder();
    ArrayList<ClassInfo> classes = new ArrayList<>(_mapNameClasses.values());
    classes.sort(new Comparator<ClassInfo>() {
      @Override
      public int compare(ClassInfo o1, ClassInfo o2) {
        final String mapPkg1 = RenamerUtil.getClassPackage(o1._mapName);
        final String mapPkg2 = RenamerUtil.getClassPackage(o2._mapName);
        int ret = NaturalOrderStringComparator.staticCompare(mapPkg1, mapPkg2);
        if (ret == 0) {
          ret = NaturalOrderStringComparator.staticCompare(o1._mapName, o2._mapName);
        }
        return ret;
      }
    });
    for (ClassInfo classInfo : classes) {
      sb.append(classInfo._orgName.replace('/', '.'))
        .append(" -> ")
        .append(classInfo._mapName.replace('/', '.'))
        .append(":\n");

      if (classInfo._fields != null) {
        for (FieldInfo fieldInfo : classInfo._fields) {
          sb.append("    ")
            .append(JavaTypes.typeDescriptorToJavaType(getOrgTypeDescriptor(fieldInfo._mapDesc)))
            .append(" ")
            .append(fieldInfo._orgName)
            .append(" -> ")
            .append(fieldInfo._mapName)
            .append("\n");
        }
      }

      if (classInfo._methods != null) {
        for (MethodInfo methodInfo : classInfo._methods) {
          sb.append("    ")
            .append(getOrgJavaMethod(methodInfo._mapDesc, methodInfo._orgName))
            .append(" -> ")
            .append(methodInfo._mapName)
            .append("\n");
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
