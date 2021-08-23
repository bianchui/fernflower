// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.bianchui.ff.renamer;

import com.github.bianchui.ff.pgmapping.MappingReader;
import com.github.bianchui.ff.utils.ClassUtil;
import com.github.bianchui.ff.utils.MyLogger;
import com.github.bianchui.ff.utils.RenamerUtil;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructInnerClassesAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructSourceFileAttribute;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MappingRenamer implements IIdentifierRenamer {
  private static MappingRenamer gInstance;
  private final ShortRenamer _shortRenamer;
  private final MappingReader _mappingReader;
  private boolean _redirectPackage = false;
  private final Map<String, String> _sourceFiles = new HashMap<>();

  public static MappingRenamer getInstance() {
    return gInstance;
  }

  public MappingRenamer() {
    gInstance = this;
    _shortRenamer = new ShortRenamer();
    File file = new File("mapping.txt");
    if (file.canRead()) {
      _mappingReader = new MappingReader(file);
      //_mappingReader.dump();
    } else {
      _mappingReader = null;
    }
  }

  public void parseStructContext(StructContext context) {
    Map<String, StructClass> classes = context.getClasses();
    Set<String> innerClasses = new HashSet<>();
    Map<String, String> mapGuessNameToMapName = new HashMap<>();
    Set<String> multiGuessName = new HashSet<>();
    for (StructClass cl : classes.values()) {
      if (!cl.isOwn()) {
        continue;
      }
      final StructInnerClassesAttribute inner = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_INNER_CLASSES);
      if (inner != null) {
        for (StructInnerClassesAttribute.Entry entry : inner.getEntries()) {
          innerClasses.add(entry.innerName);
        }
      }
    }
    for (StructClass cl : classes.values()) {
      if (!cl.isOwn()) {
        continue;
      }
      if (innerClasses.contains(cl.qualifiedName)) {
        continue;
      }

      final StructSourceFileAttribute sourceFile = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_SOURCE_FILE);
      if (sourceFile != null) {
        final String fileName = sourceFile.getSourceFile();
        if (fileName != null) {
          if (fileName.endsWith(".java")) {
            final String guessClassName = fileName.substring(0, fileName.length() - 5).trim();

            final String mapPkg = RenamerUtil.getClassPackage(cl.qualifiedName);
            String orgPkg = mapPkg;
            if (_mappingReader != null) {
              String pkg = _mappingReader.getOrgPackage(mapPkg);
              if (pkg != null) {
                orgPkg = pkg;
              }
            }
            final String guessQualifiedName = orgPkg + "/" + guessClassName;
            if (!multiGuessName.contains(guessQualifiedName)) {
              if (mapGuessNameToMapName.containsKey(guessQualifiedName)) {
                multiGuessName.add(guessQualifiedName);
                mapGuessNameToMapName.remove(guessQualifiedName);
              } else {
                mapGuessNameToMapName.put(guessQualifiedName, cl.qualifiedName);
              }
            }
          }
        }
      }

      // then add all guessed name to map
      for (Map.Entry<String, String> entry : mapGuessNameToMapName.entrySet()) {
        MyLogger.guess_name_log("guess %s -> %s\n", entry.getValue(), entry.getKey());
      }
    }
  }

  private void redirectPackage() {
    PoolInterceptor interceptor = DecompilerContext.getPoolInterceptor();
    interceptor.renameClass("android/support/v7/widget/LinearLayoutManager", "androidx/recyclerview/widget/LinearLayoutManager");
    interceptor.renameClass("android/support/v7/widget/RecyclerView", "androidx/recyclerview/widget/RecyclerView");
    interceptor.renameClass("android/support/v4/view/ViewPager", "androidx/viewpager/widget/ViewPager");
    interceptor.redirectPackage("android/support/annotation", "androidx/annotation");
    interceptor.redirectPackage("android/support/v7", "androidx/appcompat");
    interceptor.redirectPackage("android/support/v4", "androidx/core");
  }

  @Override
  public String renamePackage(String oldParentPackage, String newParentPackage, String oldSubName) {
    if (!_redirectPackage) {
      redirectPackage();
      _redirectPackage = true;
    }
    if (_mappingReader != null) {
      String orgPackage = _mappingReader.getOrgPackage(RenamerUtil.concatPackage(oldParentPackage, oldSubName));
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
