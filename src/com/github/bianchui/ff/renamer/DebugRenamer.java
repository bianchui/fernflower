package com.github.bianchui.ff.renamer;

import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;

public class DebugRenamer implements IIdentifierRenamer {
  private final IIdentifierRenamer _renamer = new MappingGenRenamer();

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    String newSubName = _renamer.renamePackage(oldParentPackage, newParentPackage, oldSubName);
    if (!oldSubName.equals(newSubName) || !newParentPackage.equals(oldParentPackage)) {
      if (oldParentPackage.length() == 0) {
        System.out.printf("Rename package: %s -> %s\n", oldSubName, newSubName);
      } else {
        System.out.printf("Rename package: %s/%s -> %s/%s\n", oldParentPackage, oldSubName, newParentPackage, newSubName);
      }
    }
    return newSubName;
  }

  /**
   *
   * @param elementType
   *    Type.ELEMENT_CLASS:
   *      renameClass pass:
   *        className: shortName
   *        element: canonicalName without package change
   *        descriptor: null
   *      innerClass pass:
   *        className: shortInnerName
   *        element: canonicalName with package change
   *        descriptor: null
   *    Type.ELEMENT_FIELD:
   *      className: canonicalName
   *    Type.ELEMENT_METHOD:
   *
   * @param className
   * @param element
   * @param descriptor
   * @return
   */
  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    boolean rename = _renamer.toBeRenamed(elementType, className, element, descriptor);
    System.out.printf("Renamer.toBeRenamed(%s, %s: %s, %s) -> %s\n", elementType.name(), className, element, descriptor, rename);
    return rename;
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    String newName = _renamer.getNextClassName(fullName, shortName);
    System.out.printf("Renamer.getNextClassName(%s, %s) -> %s\n", fullName, shortName, newName);
    return newName;
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String newName = _renamer.getNextFieldName(className, field, descriptor);
    System.out.printf("Renamer.getNextFieldName(%s, %s, %s) -> %s\n", className, field, descriptor, newName);
    return newName;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    String newName = _renamer.getNextMethodName(className, method, descriptor);
    System.out.printf("Renamer.getNextMethodName(%s, %s, %s) -> %s\n", className, method, descriptor, newName);
    return newName;
  }
}
