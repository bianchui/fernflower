package com.github.bianchui.ff.renamer;

import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.modules.renamer.ConverterHelper;

public class ShortRenamer implements IIdentifierRenamer {
  private final ConverterHelper _helper = new ConverterHelper();

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    return oldSubName;
  }

  private boolean isRenameAllPass(String shortName, String fullName) {
    if (!fullName.endsWith(shortName)) {
      return false;
    }
    if (fullName.length() == shortName.length()) {
      return true;
    }
    return fullName.charAt(fullName.length() - shortName.length() - 1) == '/';
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    if (elementType == Type.ELEMENT_CLASS) {
      final boolean renameAllPass = isRenameAllPass(className, element);
      if (renameAllPass) {
        // rename all class pass

        // try any part of class name "Class$Inner$Inner_Inner" to be renamed
        int nameStart = 0;
        while (true) {
          final int nameEnd = className.indexOf('$', nameStart);
          final String subName = className.substring(nameStart, nameEnd == -1 ? className.length() : nameEnd);
          if (_helper.toBeRenamed(Type.ELEMENT_CLASS, subName, null, null)) {
            return true;
          }
          if (nameEnd == -1) {
            return false;
          }
          nameStart = nameEnd + 1;
        }
      } else {
        // rename inner pass

        return !element.endsWith("$" + className);
      }

    }
    return _helper.toBeRenamed(elementType, className, element, descriptor);
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    final boolean renameAllPass = isRenameAllPass(shortName, fullName);
    if (renameAllPass) {
      // rename all class pass

      // try any part of class name "Class$Inner$Inner_Inner" to be renamed
      int nameStart = 0;
      int innerLevel = 0;
      StringBuilder sb = new StringBuilder();
      while (true) {
        final int nameEnd = shortName.indexOf('$', nameStart);
        final String subName = shortName.substring(nameStart, nameEnd == -1 ? shortName.length() : nameEnd);
        if (innerLevel > 0) {
          sb.append('$');
        }
        if (_helper.toBeRenamed(Type.ELEMENT_CLASS, subName, null, null)) {
          if (innerLevel > 0) {
            for (int i = 0; i < innerLevel; ++i) {
              sb.append("Inner_");
            }
          } else {
            sb.append("Class_");
          }
          sb.append(subName);
        } else {
          sb.append(subName);
        }
        ++innerLevel;
        if (nameEnd == -1) {
          break;
        }
        nameStart = nameEnd + 1;
      }
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
    return _helper.getNextFieldName(className, field, descriptor);
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    return _helper.getNextMethodName(className, method, descriptor);
  }
}
