package com.github.bianchui.ff.generic;

import org.jetbrains.java.decompiler.main.ClassWriter;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericClassDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;

import java.util.HashMap;
import java.util.Map;

public class GenericContext {
  private final Map<String, GenericClassInfo> _genericClassInfoMap = new HashMap<>();
  public GenericClassInfo loadGenericClassInfo(StructClass cl) {
    GenericClassInfo genericClassInfo = _genericClassInfoMap.get(cl.qualifiedName);
    if (genericClassInfo != null) {
      return genericClassInfo;
    }
    GenericClassDescriptor descriptor = null;
    StructGenericSignatureAttribute attr = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_SIGNATURE);
    if (attr != null) {
      descriptor = GenericMain.parseClassSignature(attr.getSignature());
    }
    if (descriptor != null) {
      genericClassInfo = new GenericClassInfo(cl, descriptor);
      _genericClassInfoMap.put(cl.qualifiedName, genericClassInfo);
    }
    return genericClassInfo;
  }

  public GenericClassInfo getInfo(String className) {
    GenericClassInfo classInfo = _genericClassInfoMap.get(className);
    if (classInfo != null) {
      classInfo.fixParentClass(this);
    }
    return classInfo;
  }
}
