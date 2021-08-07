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
      //_mappingReader.dump();
    } else {
      _mappingReader = null;
    }
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    if (true) {
      return oldSubName;
    }
    String newSubName = _helper.renamePackage(oldParentPackage, newParentPackage, oldSubName);
    return newSubName;
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    if (_mappingReader != null) {
      switch (elementType) {
        case ELEMENT_CLASS: {
          if (_mappingReader.getClassOrgName(element) != null) {
            return true;
          }
          break;
        }
        case ELEMENT_FIELD: {
          String orgName = _mappingReader.getFieldOrgName(className, element, descriptor);
          if (orgName != null && !orgName.equals(element)) {
            return true;
          }
          break;
        }
        case ELEMENT_METHOD: {
          String orgName = _mappingReader.getMethodOrgName(className, element, descriptor);
          if (orgName != null && !orgName.equals(element)) {
            return true;
          }
          break;
        }
      }
    }
    return _helper.toBeRenamed(elementType, className, element, descriptor);
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    String orgName = _mappingReader.getClassOrgName(fullName);
    if (orgName == null) {
      return _helper.getNextClassName(fullName, shortName);
    }
    int i = orgName.lastIndexOf('/');
    orgName = orgName.substring(i + 1);
    return orgName;
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String orgName = _mappingReader.getFieldOrgName(className, field, descriptor);
    if (orgName == null) {
      return _helper.getNextFieldName(className, field, descriptor);
    }
    return orgName;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    String orgName = _mappingReader.getMethodOrgName(className, method, descriptor);
    if (orgName == null) {
      return _helper.getNextMethodName(className, method, descriptor);
    }
    return orgName;
  }
}
