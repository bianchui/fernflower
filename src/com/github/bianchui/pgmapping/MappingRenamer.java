package com.github.bianchui.pgmapping;

import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.modules.renamer.ConverterHelper;

import java.io.File;

public class MappingRenamer implements IIdentifierRenamer {
  private final ConverterHelper _helper;
  private final MappingReader _mappingReader;

  public MappingRenamer() {
    _helper = new ConverterHelper();
    File file = new File("mapping.txt");
    if (file.canRead()) {
      _mappingReader = new MappingReader(file);
      _mappingReader.dump();
    } else {
      _mappingReader = null;
    }
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
    System.out.printf("toBeRenamed(%s) %s: %s, %s\n", elementType.name(), className, element, descriptor);
    if (_mappingReader != null) {
      switch (elementType) {
        case ELEMENT_CLASS: {
          if (_mappingReader.getClassOrgName(element) != null) {
            return true;
          }
          break;
        }
        case ELEMENT_FIELD:
          if (_mappingReader.getFieldOrgName(className, element, descriptor) != null) {
            return true;
          }
          break;
      }
    }
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
