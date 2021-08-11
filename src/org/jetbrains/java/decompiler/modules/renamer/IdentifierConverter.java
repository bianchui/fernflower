// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.renamer;

import com.github.bianchui.ff.utils.MyLogger;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.NewClassNameBuilder;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.io.IOException;
import java.util.*;

public class IdentifierConverter implements NewClassNameBuilder {
  private final StructContext context;
  private final IIdentifierRenamer helper;
  private final PoolInterceptor interceptor;
  private List<ClassWrapperNode> rootClasses = new ArrayList<>();
  private List<ClassWrapperNode> rootInterfaces = new ArrayList<>();
  private Map<String, Map<String, String>> interfaceNameMaps = new HashMap<>();

  private Map<String, String> _renamePackages = new HashMap<>();
  private Set<String> _notRenamePackages = new HashSet<>();

  public IdentifierConverter(StructContext context, IIdentifierRenamer helper, PoolInterceptor interceptor) {
    this.context = context;
    this.helper = helper;
    this.interceptor = interceptor;
  }

  public void rename() {
    try {
      System.out.printf("----- rename [start]\n");
      buildInheritanceTree();
      System.out.printf("----- rename [all classes]\n");
      renameAllClasses();
      System.out.printf("----- rename [interfaces]\n");
      renameInterfaces();
      System.out.printf("----- rename [classes]\n");
      renameClasses();
      System.out.printf("----- rename [reload]\n");
      context.reloadContext();
      System.out.printf("----- rename [end]\n");
    }
    catch (IOException ex) {
      throw new RuntimeException("Renaming failed!");
    }
  }

  private void renameClasses() {
    List<ClassWrapperNode> lstClasses = getReversePostOrderListIterative(rootClasses);
    Map<String, Map<String, String>> classNameMaps = new HashMap<>();

    for (ClassWrapperNode node : lstClasses) {
      StructClass cl = node.getClassStruct();
      Map<String, String> names = new HashMap<>();

      // merge information on super class
      if (cl.superClass != null) {
        Map<String, String> mapClass = classNameMaps.get(cl.superClass.getString());
        if (mapClass != null) {
          names.putAll(mapClass);
          fixSuperMembers(cl, cl.superClass.getString(), mapClass);
        }
      }

      // merge information on interfaces
      for (String ifName : cl.getInterfaceNames()) {
        Map<String, String> mapInt = interfaceNameMaps.get(ifName);
        if (mapInt != null) {
          names.putAll(mapInt);
        }
        else {
          StructClass clintr = context.getClass(ifName);
          if (clintr != null) {
            mapInt = processExternalInterface(clintr);
            names.putAll(mapInt);
          }
        }
        if (mapInt != null) {
          fixSuperMembers(cl, ifName, mapInt);
        }
      }

      renameClassIdentifiers(cl, names);

      if (!node.getSubclasses().isEmpty()) {
        classNameMaps.put(cl.qualifiedName, names);
      }
    }
  }

  private Map<String, String> processExternalInterface(StructClass cl) {
    Map<String, String> names = new HashMap<>();

    for (String ifName : cl.getInterfaceNames()) {
      Map<String, String> mapInt = interfaceNameMaps.get(ifName);
      if (mapInt != null) {
        names.putAll(mapInt);
      }
      else {
        StructClass clintr = context.getClass(ifName);
        if (clintr != null) {
          mapInt = processExternalInterface(clintr);
          names.putAll(mapInt);
        }
      }
      if (mapInt != null) {
        fixSuperMembers(cl, ifName, mapInt);
      }
    }

    renameClassIdentifiers(cl, names);

    return names;
  }

  private void renameInterfaces() {
    List<ClassWrapperNode> lstInterfaces = getReversePostOrderListIterative(rootInterfaces);
    Map<String, Map<String, String>> interfaceNameMaps = new HashMap<>();

    // rename methods and fields
    for (ClassWrapperNode node : lstInterfaces) {

      StructClass cl = node.getClassStruct();
      Map<String, String> names = new HashMap<>();

      // merge information on super interfaces
      for (String ifName : cl.getInterfaceNames()) {
        Map<String, String> mapInt = interfaceNameMaps.get(ifName);
        if (mapInt != null) {
          names.putAll(mapInt);
          fixSuperMembers(cl, ifName, mapInt);
        }
      }

      renameClassIdentifiers(cl, names);

      interfaceNameMaps.put(cl.qualifiedName, names);
    }

    this.interfaceNameMaps = interfaceNameMaps;
  }

  // [BC]
  private void fixSuperMembers(StructClass thisCls, String superClsName, Map<String, String> names) {
    String newClsName = interceptor.getName(thisCls.qualifiedName);
    if (newClsName == null) {
      newClsName = thisCls.qualifiedName;
    }

    for (Map.Entry<String, String> entry : names.entrySet()) {
      final String key = entry.getKey();
      String[] keys = key.split(" ");
      if (keys.length == 2) {
        if (!keys[0].equals(entry.getValue())) {
          final String superNewName = interceptor.getName(superClsName + " " + key);
          String[] newNames = superNewName.split(" ");
          final String oldName = thisCls.qualifiedName + " " + key;
          final String newName = newClsName + " " + newNames[1] + " " + newNames[2];
          if (interceptor.getName(oldName) == null) {
            MyLogger.rename_log("Fix name map %s -> %s\n", oldName, newName);
          }
          interceptor.addName(oldName, newName);
        }
      }
    }
  }

  private void renameAllClasses() {
    // order not important
    List<ClassWrapperNode> lstAllClasses = new ArrayList<>(getReversePostOrderListIterative(rootInterfaces));
    lstAllClasses.addAll(getReversePostOrderListIterative(rootClasses));

    // rename all interfaces and classes
    for (ClassWrapperNode node : lstAllClasses) {
      renameClass(node.getClassStruct());
    }
  }

  private void renameClass(StructClass cl) {

    if (!cl.isOwn()) {
      return;
    }

    String classOldFullName = cl.qualifiedName;
    String classFullName = classOldFullName;
    boolean packageChange = false;

    // TODO: rename packages
    {
      String lastOldPackageName = "";
      String lastNewPackageName = lastOldPackageName;
      for (int i = 0; i < classOldFullName.length(); ) {
        int nextPackage = classOldFullName.indexOf('/', i);
        if (nextPackage == -1) {
          if (packageChange) {
            classFullName = lastNewPackageName + '/' + classOldFullName.substring(i);
          }
          break;
        }
        final String packageName = classOldFullName.substring(0, nextPackage);
        if (packageChange || !_notRenamePackages.contains(packageName)) {
          if (_renamePackages.containsKey(packageName)) {
            lastNewPackageName = _renamePackages.get(packageName);
            packageChange = true;
          } else {
            final String subPackageName = lastOldPackageName.length() == 0 ? packageName : packageName.substring(lastOldPackageName.length() + 1);
            final String newSubName = helper.renamePackage(lastOldPackageName, lastNewPackageName, subPackageName);
            if (packageChange || !newSubName.equals(subPackageName)) {
              if (lastOldPackageName.length() == 0) {
                lastNewPackageName = newSubName;
              } else {
                lastNewPackageName += '/' + newSubName;
              }
              packageChange = true;
              _renamePackages.put(packageName, lastNewPackageName);
            } else {
              lastNewPackageName = packageName;
              _notRenamePackages.add(packageName);
            }
          }
        } else {
          lastNewPackageName = packageName;
        }
        lastOldPackageName = packageName;
        i = nextPackage + 1;
      }
    }


    String clSimpleName = ConverterHelper.getSimpleClassName(classFullName);
    if (helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_CLASS, clSimpleName, classOldFullName, null)) {
      String classNewFullName;

      do {
        String classname = helper.getNextClassName(classOldFullName, ConverterHelper.getSimpleClassName(classFullName));
        classNewFullName = ConverterHelper.replaceSimpleClassName(classFullName, classname);
      }
      while (context.getClasses().containsKey(classNewFullName));

      interceptor.addName(classOldFullName, classNewFullName);
      System.out.printf("Rename class: %s -> %s\n", classOldFullName, classNewFullName);
    } else {
      if (packageChange) {
        interceptor.addName(classOldFullName, classFullName);
        System.out.printf("Rename class: %s -> %s\n", classOldFullName, classFullName);
      }
    }
  }

  // [BC] add for overload
  private static String getParamsDescriptor(String desc) {
    final int last = desc.lastIndexOf(')');
    if (last >= 0) {
      desc = desc.substring(0, last + 1);
    }
    return desc;
  }

  private void renameClassIdentifiers(StructClass cl, Map<String, String> names) {
    // all classes are already renamed
    String classOldFullName = cl.qualifiedName;
    String classNewFullName = interceptor.getName(classOldFullName);

    if (classNewFullName == null) {
      classNewFullName = classOldFullName;
    }

    // methods
    HashSet<String> setMethodNames = new HashSet<>();
    for (StructMethod md : cl.getMethods()) {
      // [BC] change for overload
      final String paramsDescriptor = getParamsDescriptor(md.getDescriptor());
      setMethodNames.add(md.getName() + ' ' + paramsDescriptor);
    }

    VBStyleCollection<StructMethod, String> methods = cl.getMethods();
    for (int i = 0; i < methods.size(); i++) {

      StructMethod mt = methods.get(i);
      String key = methods.getKey(i);

      boolean isPrivate = mt.hasModifier(CodeConstants.ACC_PRIVATE);

      String name = mt.getName();
      if (!cl.isOwn() || mt.hasModifier(CodeConstants.ACC_NATIVE)) {
        // external and native methods must not be renamed
        if (!isPrivate) {
          names.put(key, name);
        }
      }
      else if (!names.containsKey(key) && helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_METHOD, classOldFullName, name, mt.getDescriptor())) {
        if (isPrivate || !names.containsKey(key)) {
          final String paramsDescriptor = getParamsDescriptor(mt.getDescriptor());
          do {
            name = helper.getNextMethodName(classOldFullName, name, mt.getDescriptor());
          }
          while (setMethodNames.contains(name + ' ' + paramsDescriptor));
          setMethodNames.add(name + ' ' + paramsDescriptor);

          if (!isPrivate) {
            names.put(key, name);
          }
        }
        else {
          name = names.get(key);
        }

        interceptor.addName(classOldFullName + " " + mt.getName() + " " + mt.getDescriptor(),
                            classNewFullName + " " + name + " " + buildNewDescriptor(false, mt.getDescriptor()));
      }
    }

    // external fields are not being renamed
    if (!cl.isOwn()) {
      return;
    }

    // fields
    // FIXME: should overloaded fields become the same name?
    HashSet<String> setFieldNames = new HashSet<>();
    for (StructField fd : cl.getFields()) {
      setFieldNames.add(fd.getName());
    }

    for (StructField fd : cl.getFields()) {
      if (helper.toBeRenamed(IIdentifierRenamer.Type.ELEMENT_FIELD, classOldFullName, fd.getName(), fd.getDescriptor())) {
        String newName;
        do {
          newName = helper.getNextFieldName(classOldFullName, fd.getName(), fd.getDescriptor());
        }
        while (setFieldNames.contains(newName));

        interceptor.addName(classOldFullName + " " + fd.getName() + " " + fd.getDescriptor(),
                            classNewFullName + " " + newName + " " + buildNewDescriptor(true, fd.getDescriptor()));

        boolean isPrivate = fd.hasModifier(CodeConstants.ACC_PRIVATE);

        if (!isPrivate) {
          names.put(fd.getName() + " " + fd.getDescriptor(), newName);
        }
      }
    }
  }

  @Override
  public String buildNewClassname(String className) {
    return interceptor.getName(className);
  }

  private String buildNewDescriptor(boolean isField, String descriptor) {
    String newDescriptor;
    if (isField) {
      newDescriptor = FieldDescriptor.parseDescriptor(descriptor).buildNewDescriptor(this);
    }
    else {
      newDescriptor = MethodDescriptor.parseDescriptor(descriptor).buildNewDescriptor(this);
    }
    return newDescriptor != null ? newDescriptor : descriptor;
  }

  private static List<ClassWrapperNode> getReversePostOrderListIterative(List<ClassWrapperNode> roots) {
    List<ClassWrapperNode> res = new ArrayList<>();

    LinkedList<ClassWrapperNode> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();

    Set<ClassWrapperNode> setVisited = new HashSet<>();

    for (ClassWrapperNode root : roots) {
      stackNode.add(root);
      stackIndex.add(0);
    }

    while (!stackNode.isEmpty()) {
      ClassWrapperNode node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      List<ClassWrapperNode> lstSubs = node.getSubclasses();

      for (; index < lstSubs.size(); index++) {
        ClassWrapperNode sub = lstSubs.get(index);
        if (!setVisited.contains(sub)) {
          stackIndex.add(index + 1);
          stackNode.add(sub);
          stackIndex.add(0);
          break;
        }
      }

      if (index == lstSubs.size()) {
        res.add(0, node);
        stackNode.removeLast();
      }
    }

    return res;
  }

  private void buildInheritanceTree() {
    Map<String, ClassWrapperNode> nodes = new HashMap<>();
    Map<String, StructClass> classes = context.getClasses();

    List<ClassWrapperNode> rootClasses = new ArrayList<>();
    List<ClassWrapperNode> rootInterfaces = new ArrayList<>();

    for (StructClass cl : classes.values()) {
      if (!cl.isOwn()) {
        continue;
      }

      LinkedList<StructClass> stack = new LinkedList<>();
      LinkedList<ClassWrapperNode> stackSubNodes = new LinkedList<>();

      stack.add(cl);
      stackSubNodes.add(null);

      while (!stack.isEmpty()) {
        StructClass clStr = stack.removeFirst();
        ClassWrapperNode child = stackSubNodes.removeFirst();

        ClassWrapperNode node = nodes.get(clStr.qualifiedName);
        boolean isNewNode = (node == null);

        if (isNewNode) {
          nodes.put(clStr.qualifiedName, node = new ClassWrapperNode(clStr));
        }

        if (child != null) {
          node.addSubclass(child);
        }

        if (!isNewNode) {
          // [BC] fix missing process parent trees
          continue;
        }
        else {
          boolean isInterface = clStr.hasModifier(CodeConstants.ACC_INTERFACE);
          boolean found_parent = false;

          if (isInterface) {
            for (String ifName : clStr.getInterfaceNames()) {
              StructClass clParent = classes.get(ifName);
              if (clParent != null) {
                stack.add(clParent);
                stackSubNodes.add(node);
                found_parent = true;
              }
            }
          }
          else if (clStr.superClass != null) { // null iff java/lang/Object
            StructClass clParent = classes.get(clStr.superClass.getString());
            if (clParent != null) {
              stack.add(clParent);
              stackSubNodes.add(node);
              found_parent = true;
            }
          }

          if (!found_parent) { // no super class or interface
            (isInterface ? rootInterfaces : rootClasses).add(node);
          }
        }
      }
    }

    this.rootClasses = rootClasses;
    this.rootInterfaces = rootInterfaces;

    Comparator<ClassWrapperNode> comparator = new Comparator<ClassWrapperNode>() {
      @Override
      public int compare(ClassWrapperNode o1, ClassWrapperNode o2) {
        return o1.getClassStruct().qualifiedName.compareTo(o2.getClassStruct().qualifiedName);
      }
    };

    System.out.println("----------------------------------------");
    System.out.println("---- Interface tree");
    System.out.println("----------------------------------------");
    dumpClssTree(rootInterfaces, "", comparator);
    System.out.println("----------------------------------------");
    System.out.println("---- Class tree");
    System.out.println("----------------------------------------");
    dumpClssTree(rootClasses, "", comparator);
    System.out.println("----------------------------------------");
  }

  private static void dumpClssTree(List<ClassWrapperNode> tree, String indent, Comparator<ClassWrapperNode> comparator) {
    if (tree == null) {
      return;
    }
    if (comparator != null) {
      tree = new ArrayList<>(tree);
      tree.sort(comparator);
    }
    for (ClassWrapperNode cls : tree) {
      System.out.printf("%s+ %s\n", indent, cls.getClassStruct().qualifiedName);
      dumpClssTree(cls.getSubclasses(), indent + "  ", comparator);
    }
  }
}