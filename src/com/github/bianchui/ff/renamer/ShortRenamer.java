// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.renamer;

import com.github.bianchui.ff.utils.JavaTypes;
import com.github.bianchui.ff.utils.RenamerUtil;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.modules.renamer.ConverterHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortRenamer implements IIdentifierRenamer {
  private final ConverterHelper _helper = new ConverterHelper();
  private final Pattern kPattern_Oo0 = Pattern.compile("[Oo0]+");

  public boolean isAll_Oo0(String name) {
    if (name.length() <= 2) {
      return false;
    }
    Matcher matcher = kPattern_Oo0.matcher(name);
    return matcher.matches();
  }

  public int nameOo0ToInt(String name) {
    int num = 0;
    for (int i = 0; i < name.length(); ++i) {
      int a = 0;
      switch (name.charAt(i)) {
        case 'O':
          a = 1;
          break;
        case 'o':
          a = 2;
          break;
        case '0':
          a = 3;
          break;
      }
      num = num << 2 | a;
    }
    return num;
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    if (isAll_Oo0(oldSubName)) {
      oldSubName = "op_" + nameOo0ToInt(oldSubName);
    }
    return oldSubName;
  }

  public boolean isClassNeedRenamed(String shortName) {
    // try any part of class name "Class$Inner$Inner_Inner" to be renamed
    int nameStart = 0;
    while (true) {
      final int nameEnd = shortName.indexOf('$', nameStart);
      final String subName = shortName.substring(nameStart, nameEnd == -1 ? shortName.length() : nameEnd);
      if (_helper.toBeRenamed(Type.ELEMENT_CLASS, subName, null, null) || isAll_Oo0(subName)) {
        return true;
      }
      if (nameEnd == -1) {
        return false;
      }
      nameStart = nameEnd + 1;
    }
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    if (elementType == Type.ELEMENT_CLASS) {
      final boolean renameAllPass = RenamerUtil.isRenameAllPass(className, element);
      if (renameAllPass) {
        // rename all class pass

        return isClassNeedRenamed(className);
      } else {
        // rename inner pass

        return !element.endsWith("$" + className);
      }

    }
    return _helper.toBeRenamed(elementType, className, element, descriptor) || isAll_Oo0(element);
  }

  public void renameInnerClass(String shortName, int nameStart, int innerLevel, StringBuilder sb) {
    while (true) {
      final int nameEnd = shortName.indexOf('$', nameStart);
      final String subName = shortName.substring(nameStart, nameEnd == -1 ? shortName.length() : nameEnd);
      if (innerLevel > 0) {
        sb.append('$');
      }
      if (_helper.toBeRenamed(Type.ELEMENT_CLASS, subName, null, null) || isAll_Oo0(subName)) {
        if (innerLevel > 0) {
          for (int i = 0; i < innerLevel; ++i) {
            sb.append("Inner_");
          }
        } else {
          sb.append("Class_");
        }
      }
      if (isAll_Oo0(subName)) {
        sb.append("OC_");
        sb.append(nameOo0ToInt(subName));
      } else {
        sb.append(subName);
      }
      ++innerLevel;
      if (nameEnd == -1) {
        break;
      }
      nameStart = nameEnd + 1;
    }
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    final boolean renameAllPass = RenamerUtil.isRenameAllPass(shortName, fullName);
    if (renameAllPass) {
      // rename all class pass

      // try any part of class name "Class$Inner$Inner_Inner" to be renamed
      StringBuilder sb = new StringBuilder();
      renameInnerClass(shortName, 0, 0, sb);
      return sb.toString();
    } else {
      // rename inner pass

      String newName = null;
      final int classNameIndex = fullName.lastIndexOf('/');
      final int innerIndex = fullName.lastIndexOf("$");
      if (innerIndex > classNameIndex + 1) {
        newName = fullName.substring(innerIndex + 1);
      } else {
        newName = "OrphanInner_" + shortName;
      }
      return newName;
    }
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    if (isAll_Oo0(field)) {
      field = "of_" + nameOo0ToInt(field);
    }
    return "_" + RenamerUtil.typeDescriptorShortName(descriptor, 0) + "_" + field;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    if (isAll_Oo0(method)) {
      method = "om_" + nameOo0ToInt(method);
    }
    if (descriptor.equals("()V")) {
      return "method_" + method;
    }
    if (descriptor.startsWith("()")) {
      return "get_" + RenamerUtil.typeDescriptorShortName(descriptor, 2) + "_" + method;
    }
    int count = JavaTypes.getMethodArgCount(descriptor);
    if (descriptor.endsWith(")V") && count == 1) {
      return "set_" + RenamerUtil.typeDescriptorShortName(descriptor, 1) + "_" + method;
    }
    return "met_" + RenamerUtil.methodDescriptorVeryShortName(descriptor) + "_" + method;
  }
}
