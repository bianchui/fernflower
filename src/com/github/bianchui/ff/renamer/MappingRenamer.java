package com.github.bianchui.ff.renamer;

import com.github.bianchui.ff.pgmapping.MappingReader;
import com.github.bianchui.ff.utils.ClassUtil;
import com.github.bianchui.ff.utils.RenamerUtil;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;

import java.io.File;

public class MappingRenamer implements IIdentifierRenamer {
  private final ShortRenamer _shortRenamer;
  private final MappingReader _mappingReader;

  public MappingRenamer() {
    _shortRenamer = new ShortRenamer();
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
    return _shortRenamer.renamePackage(oldParentPackage, newParentPackage, oldSubName);
  }

  @Override
  public boolean toBeRenamed(Type elementType, String className, String element, String descriptor) {
    if (_mappingReader != null) {
      switch (elementType) {
        case ELEMENT_CLASS: {
          final boolean renameAllPass = RenamerUtil.isRenameAllPass(className, element);
          if (renameAllPass) {
            String orgName = _mappingReader.getClassOrgName(element);
            if (orgName != null) {
              return !orgName.equals(element);
            }
            // if inner class, get outer class name test for rename
            int nameStart = element.lastIndexOf('/') + 1;
            // test all possible mapping record names
            while (true) {
              final int outerEnd = element.indexOf('$', nameStart);
              if (outerEnd == -1) {
                break;
              }
              final String outerFullName = element.substring(0, outerEnd);
              orgName = _mappingReader.getClassOrgName(outerFullName);
              if (orgName == null) {
                break;
              }
              if (!orgName.equals(outerFullName)) {
                return true;
              }
              nameStart = outerEnd + 1;
            }
            // test rest of inner name for rename
            return _shortRenamer.isClassNeedRenamed(element.substring(nameStart));
          } else {

            // in inner pass, will try to get new name for InnerClass
            final String innerName = ClassUtil.getClassInnerName(element);
            if (innerName != null) {
              return !innerName.equals(className);
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
    return _shortRenamer.toBeRenamed(elementType, className, element, descriptor);
  }

  @Override
  public String getNextClassName(final String fullName, final String shortName) {
    if (_mappingReader != null) {
      final boolean renameAllPass = RenamerUtil.isRenameAllPass(shortName, fullName);
      if (renameAllPass) {
        String orgName = _mappingReader.getClassOrgName(fullName);
        if (orgName != null) {
          return ClassUtil.getClassShortName(orgName);
        }
        // if inner class, get outer class name test for rename
        int nameStart = fullName.lastIndexOf('/') + 1;

        // test all possible mapping record names
        int innerLevel = 0;
        while (true) {
          int outerEnd = fullName.indexOf('$', nameStart);
          if (outerEnd == -1) {
            break;
          }
          final String outerFullName = fullName.substring(0, outerEnd);
          final String curOrgName = _mappingReader.getClassOrgName(outerFullName);
          if (curOrgName == null) {
            break;
          }
          orgName = curOrgName;
          ++innerLevel;
          nameStart = outerEnd + 1;
        }
        // test rest of inner name for rename
        StringBuilder sb = new StringBuilder();
        if (orgName != null) {
          sb.append(ClassUtil.getClassShortName(orgName));
        }
        _shortRenamer.renameInnerClass(fullName.substring(nameStart), 0, innerLevel, sb);
        return sb.toString();
      } else {
        // in inner pass, name already in fullName so just get it
        final int innerIndex = fullName.lastIndexOf('$');
        if (innerIndex != -1) {
          return fullName.substring(innerIndex + 1);
        }
      }
    }
    return _shortRenamer.getNextClassName(fullName, shortName);
  }

  @Override
  public String getNextFieldName(String className, String field, String descriptor) {
    String orgName = null;
    if (_mappingReader != null) {
      orgName = _mappingReader.getFieldOrgName(className, field, descriptor);
    }
    if (orgName == null) {
      return _shortRenamer.getNextFieldName(className, field, descriptor);
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
      return _shortRenamer.getNextMethodName(className, method, descriptor);
    }
    return orgName;
  }
}
