package com.github.bianchui.ff.renamer;

import com.github.bianchui.ff.pgmapping.MappingReader;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;

import java.io.File;

public class MappingRenamer implements IIdentifierRenamer {
  private final IIdentifierRenamer _fallbackRenamer;
  private final MappingReader _mappingReader;

  public MappingRenamer() {
    _fallbackRenamer = new ShortRenamer();
    File file = new File("mapping.txt");
    if (file.canRead()) {
      _mappingReader = new MappingReader(file);
      //_mappingReader.dump();
    } else {
      _mappingReader = null;
    }
  }

  private static String concatPackage(String parent, String subName) {
    return parent == null || parent.length() == 0 ? subName : parent + '/' + subName;
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    if (_mappingReader != null) {
      String orgPackage = _mappingReader.getOrgPackage(concatPackage(oldParentPackage, oldSubName));
      if (orgPackage != null) {
        return orgPackage.substring(orgPackage.lastIndexOf('/') + 1);
      }
    }
    return _fallbackRenamer.renamePackage(oldParentPackage, newParentPackage, oldSubName);
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    if (_mappingReader != null) {
      switch (elementType) {
        case ELEMENT_CLASS: {
          String orgName = _mappingReader.getClassOrgName(element);
          if (orgName != null) {
            //if (orgName.lastIndexOf('$') != -1) {
            //  return false;
            //}
            return !orgName.equals(element);
          }
          // in inner pass, will try to get new name for InnerClass
          final int innerIndex = element.lastIndexOf('$');
          if (innerIndex != -1) {
            String mapName = _mappingReader.getClassMapName(element);
            if (mapName != null) {
              final int mapInnerIndex = mapName.lastIndexOf('$');
              if (mapInnerIndex == -1) {
                return true;
              }
              String orgInner = element.substring(innerIndex + 1);
              String mapInner = mapName.substring(mapInnerIndex + 1);
              return !orgInner.equals(mapInner);
            }
          }
          break;
        }
        case ELEMENT_FIELD: {
          String orgName = _mappingReader.getFieldOrgName(className, element, descriptor);
          if (orgName != null) {
            return !orgName.equals(element);
          }
          break;
        }
        case ELEMENT_METHOD: {
          String orgName = _mappingReader.getMethodOrgName(className, element, descriptor);
          if (orgName != null) {
            return !orgName.equals(element);
          }
          break;
        }
      }
    }
    return _fallbackRenamer.toBeRenamed(elementType, className, element, descriptor);
  }

  @Override
  public String getNextClassName(String fullName, String shortName) {
    String orgName = null;
    if (_mappingReader != null) {
      orgName = _mappingReader.getClassOrgName(fullName);
      if (orgName == null) {
        // in inner pass, will try to get new name for InnerClass
        final int innerIndex = fullName.lastIndexOf('$');
        if (innerIndex != -1) {
          String mapName = _mappingReader.getClassMapName(fullName);
          if (mapName != null) {
            String orgInner = fullName.substring(innerIndex + 1);
            return orgInner;
          }
        }
      }
    }
    if (orgName == null) {
      return _fallbackRenamer.getNextClassName(fullName, shortName);
    }
    int i = orgName.lastIndexOf('/');
    orgName = orgName.substring(i + 1);
    return orgName;
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String orgName = null;
    if (_mappingReader != null) {
      orgName = _mappingReader.getFieldOrgName(className, field, descriptor);
    }
    if (orgName == null) {
      return _fallbackRenamer.getNextFieldName(className, field, descriptor);
    }
    return orgName;
  }

  @Override
  public String getNextMethodName(String className, String method, String descriptor) {
    String orgName = null;
    if (_mappingReader != null) {
      orgName = _mappingReader.getMethodOrgName(className, method, descriptor);
    }
    if (orgName == null) {
      return _fallbackRenamer.getNextMethodName(className, method, descriptor);
    }
    return orgName;
  }
}
