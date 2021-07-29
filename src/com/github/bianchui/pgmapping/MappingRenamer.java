package com.github.bianchui.pgmapping;

import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.modules.renamer.ConverterHelper;

public class MappingRenamer implements IIdentifierRenamer {
  private final ConverterHelper _helper;

  public MappingRenamer() {
    _helper = new ConverterHelper();
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    String newSubName = _helper.renamePackage(oldParentPackage, newParentPackage, oldSubName);
    if (!oldSubName.equals(newSubName) || !newParentPackage.equals(oldParentPackage)) {
      if (oldParentPackage.length() == 0) {
        System.out.printf("Rename package: %s -> %s\n", oldSubName, newSubName);
      } else {
        System.out.printf("Rename package: %s/%s -> %s/%s\n", oldParentPackage, oldSubName, newParentPackage, newSubName);
      }
    }
    return newSubName;
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    return _helper.toBeRenamed(elementType, className, element, descriptor);
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    String newName = _helper.getNextClassName(fullName, shortName);
    System.out.printf("Next class: %s(%s) -> %s\n", fullName, shortName, newName);
    return newName;
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String newName = _helper.getNextFieldName(className, field, descriptor);
    return newName;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    String newName = _helper.getNextMethodName(className, method, descriptor);
    return newName;
  }
}
