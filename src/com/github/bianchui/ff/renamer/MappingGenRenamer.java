// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.renamer;

import com.github.bianchui.ff.pgmapping.MappingGen;
import com.github.bianchui.ff.utils.RenamerUtil;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;

public class MappingGenRenamer implements IIdentifierRenamer {
  private static MappingGenRenamer gInstance;
  private final IIdentifierRenamer _renamer = new MappingRenamer();
  private final MappingGen _mappingGen = new MappingGen();

  public MappingGenRenamer() {
    gInstance = this;
  }

  public static MappingGenRenamer getInstance() {
    return gInstance;
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    String newSubName = _renamer.renamePackage(oldParentPackage, newParentPackage, oldSubName);
    _mappingGen.addPackageMap(RenamerUtil.concatPackage(newParentPackage, newSubName), RenamerUtil.concatPackage(oldParentPackage, oldSubName));
    return newSubName;
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    boolean rename = _renamer.toBeRenamed(elementType, className, element, descriptor);
    final boolean renameAllPass = RenamerUtil.isRenameAllPass(className, element);
    // record name not change
    if (!rename && renameAllPass) {
      switch (elementType) {
        case ELEMENT_CLASS:
          _mappingGen.addMapClass(className, element);
          break;
        case ELEMENT_FIELD:
          _mappingGen.addMapField(className, element, element, descriptor);
          break;
        case ELEMENT_METHOD:
          _mappingGen.addMapMethod(className, element, element, descriptor);
          break;
      }
    }
    return rename;
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    final boolean renameAllPass = RenamerUtil.isRenameAllPass(shortName, fullName);

    String newName = _renamer.getNextClassName(fullName, shortName);
    if (renameAllPass) {
      // record name change
      _mappingGen.addMapClass(newName, fullName);
    }
    return newName;
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String newName = _renamer.getNextFieldName(className, field, descriptor);
    _mappingGen.addMapField(className, newName, field, descriptor);
    return newName;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    String newName = _renamer.getNextMethodName(className, method, descriptor);
    _mappingGen.addMapMethod(className, newName, method, descriptor);
    return newName;
  }

  public void recordRename(Type elementType, String className, String element, String newName, String descriptor) {
    switch (elementType) {
      case ELEMENT_CLASS:
        _mappingGen.addMapClass(className, element);
        break;
      case ELEMENT_FIELD:
        _mappingGen.addMapField(className, newName, element, descriptor);
        break;
      case ELEMENT_METHOD:
        _mappingGen.addMapMethod(className, newName, element, descriptor);
        break;
    }
  }

  public String genMap() {
    return _mappingGen.genMap();
  }
}
