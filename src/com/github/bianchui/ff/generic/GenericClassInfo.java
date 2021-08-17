package com.github.bianchui.ff.generic;

import com.github.bianchui.ff.utils.JavaTypes;
import com.github.bianchui.ff.utils.MyLogger;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericClassDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericClassInfo {
  public final StructClass cl;
  public final GenericClassDescriptor descriptor;
  public final Map<String, GenericFunctionInfo> genericFunctions = new HashMap<>();
  public final Map<String, AppliedFunctionInfo> appliedFunctions = new HashMap<>();
  private boolean _parentFixed = false;

  GenericClassInfo(StructClass cl, GenericClassDescriptor classDescriptor) {
    this.cl = cl;
    this.descriptor = classDescriptor;
    // find all functions is generic
    for (StructMethod mt : cl.getMethods()) {
      // static function cannot override
      if (mt.hasModifier(CodeConstants.ACC_STATIC)) {
        continue;
      }
      if (CodeConstants.INIT_NAME.equals(mt.getName())) {
        continue;
      }
      GenericMethodDescriptor descriptor = null;
      StructGenericSignatureAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_SIGNATURE);
      if (attr != null) {
        descriptor = GenericMain.parseMethodSignature(attr.getSignature());
        if (descriptor != null) {
          long actualParams = JavaTypes.getMethodArgCount(mt.getDescriptor());
          if (actualParams != descriptor.parameterTypes.size()) {
            String message = "Inconsistent generic signature in method " + mt.getName() + " " + mt.getDescriptor() + " in " + cl.qualifiedName;
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
            descriptor = null;
          }
        }
      }
      if (descriptor == null) {
        continue;
      }
      // TODO: test typeParameters
      if (!descriptor.typeParameters.isEmpty()) {
        continue;
      }

      boolean isGeneric = false;
      for (int i = 0; i < descriptor.parameterTypes.size(); ++i) {
        GenericType param = descriptor.parameterTypes.get(i);
        if (param.type == CodeConstants.TYPE_GENVAR) {
          isGeneric = true;
          break;
        }
      }
      if (descriptor.returnType.type == CodeConstants.TYPE_GENVAR) {
        isGeneric = true;
      }

      if (isGeneric) {
        GenericFunctionInfo genericFunctionInfo = new GenericFunctionInfo(mt, descriptor);
        genericFunctions.put(genericFunctionInfo.getKey(), genericFunctionInfo);
      }
    }
  }

  void fixParentClass(GenericContext context) {
    // this must do when all classes is parsed
    if (_parentFixed) {
      return;
    }
    _parentFixed = true;
    final GenericClassDescriptor descriptor = this.descriptor;

    MyLogger.generic_log("fixing %s\n", cl.qualifiedName);

    if (descriptor.superclass != null) {
      MyLogger.generic_log("  super %s\n", descriptor.superclass.value);
      GenericClassInfo superInfo = context.getInfo(descriptor.superclass.value);
      if (superInfo != null) {
        FunctionInfo superFunctionInfo = superInfo.applyGeneric(descriptor.superclass);
        mergeSuperFunctionInfo(superFunctionInfo);
      }
    }

    for (GenericType superinterface : descriptor.superinterfaces) {
      MyLogger.generic_log("  face %s\n", superinterface.value);
      GenericClassInfo superInfo = context.getInfo(superinterface.value);
      if (superInfo != null) {
        FunctionInfo superFunctionInfo = superInfo.applyGeneric(superinterface);
        mergeSuperFunctionInfo(superFunctionInfo);
      }
    }

    MyLogger.generic_log("done %s\n", cl.qualifiedName);
  }

  void mergeSuperFunctionInfo(FunctionInfo superFunctionInfo) {
    this.genericFunctions.putAll(superFunctionInfo.genericFunctions);
    this.appliedFunctions.putAll(superFunctionInfo.appliedFunctions);
  }

  // old descriptor -> new descriptor
  FunctionInfo applyGeneric(GenericType subClassGenericType) {
    FunctionInfo functionInfo = new FunctionInfo();
    if (this.descriptor.fparameters.size() != subClassGenericType.getArguments().size()) {
      MyLogger.generic_log("ERROR: %s apply %d vs need %d\n", this.cl.qualifiedName, subClassGenericType.getArguments().size(), this.descriptor.fparameters.size());
      return functionInfo;
    }
    Map<String, GenericType> arguments = new HashMap<>();
    for (int i = 0; i < this.descriptor.fparameters.size(); ++i) {
      arguments.put(this.descriptor.fparameters.get(i), subClassGenericType.getArguments().get(i));
    }

    functionInfo.appliedFunctions.putAll(this.appliedFunctions);
    for (Map.Entry<String, GenericFunctionInfo> entry : this.genericFunctions.entrySet()) {
      final GenericFunctionInfo func = entry.getValue();
      final GenericMethodDescriptor oldGmd = func.genericMethodDescriptor;

      boolean isGeneric = false;

      // 未处理
      //List<String> typeParameters = gmd.typeParameters;
      //List<List<GenericType>> typeParameterBounds = gmd.typeParameterBounds;
      StringBuilder new_md = new StringBuilder();
      new_md.append("(");

      List<GenericType> parameterTypes = new ArrayList<>();
      GenericType returnType = oldGmd.returnType;
      List<GenericType> exceptionTypes = new ArrayList<>();

      int descI = 1;
      for (int i = 0; i < oldGmd.parameterTypes.size(); ++i) {
        GenericType param = oldGmd.parameterTypes.get(i);
        int descEnd = JavaTypes.getDescriptorEnd(func.newMethodDescriptor, descI);
        String paramDesc = func.newMethodDescriptor.substring(descI, descEnd);
        if (param.type == CodeConstants.TYPE_GENVAR) {
          GenericType value = arguments.get(param.value);
          if (value != null) {
            param = value;
            if (value.type == CodeConstants.TYPE_GENVAR) {
              isGeneric = true;
            } else {
              paramDesc = GenericUtil.toDesc(value);
            }
          } else {
            isGeneric = true;
          }
        }
        parameterTypes.add(param);
        descI = descEnd;
        new_md.append(paramDesc);
      }
      new_md.append(")");
      descI++;

      String retDesc = func.newMethodDescriptor.substring(descI);
      if (oldGmd.returnType.type == CodeConstants.TYPE_GENVAR) {
        GenericType value = arguments.get(oldGmd.returnType.value);
        if (value != null) {
          returnType = value;
          if (value.type == CodeConstants.TYPE_GENVAR) {
            isGeneric = true;
          } else {
            retDesc = GenericUtil.toDesc(value);
          }
        } else {
          isGeneric = true;
        }
      }
      new_md.append(retDesc);

      // exceptionTypes just ignore
      if (isGeneric) {
        GenericMethodDescriptor newGmd = new GenericMethodDescriptor(oldGmd.typeParameters, oldGmd.typeParameterBounds, parameterTypes, returnType, exceptionTypes);
        GenericFunctionInfo newFunc = new GenericFunctionInfo(func.methodName, func.newMethodDescriptor, new_md.toString(), newGmd);
        functionInfo.genericFunctions.put(newFunc.getKey(), newFunc);
      } else {
        AppliedFunctionInfo newFunc = new AppliedFunctionInfo(func.methodName, func.newMethodDescriptor, new_md.toString());
        functionInfo.appliedFunctions.put(newFunc.getKey(), newFunc);
      }

      MyLogger.generic_log("    func %s apply %s -> %s\n", func.methodName, func.newMethodDescriptor, new_md.toString());
    }

    return functionInfo;
  }

  @Override
  public String toString() {
    return cl.qualifiedName;
  }

  public static class GenericFunctionInfo {
    public final String methodName;
    public final String orgMethodDescriptor;
    public final String newMethodDescriptor;
    public final GenericMethodDescriptor genericMethodDescriptor;

    GenericFunctionInfo(StructMethod mt, GenericMethodDescriptor genericMethodDescriptor) {
      this(mt.getName(), mt.getDescriptor(), mt.getDescriptor(), genericMethodDescriptor);
    }

    GenericFunctionInfo(String methodName, String orgMD, String newMD, GenericMethodDescriptor genericMethodDescriptor) {
      this.methodName = methodName;
      this.orgMethodDescriptor = orgMD;
      this.newMethodDescriptor = newMD;
      this.genericMethodDescriptor = genericMethodDescriptor;
    }

    String getKey() {
      return methodName + " " + orgMethodDescriptor;
    }
  }

  public static class AppliedFunctionInfo {
    public final String methodName;
    public final String orgMethodDescriptor;
    public final String newMethodDescriptor;
    AppliedFunctionInfo(String methodName, String orgMD, String newMD) {
      this.methodName = methodName;
      this.orgMethodDescriptor = orgMD;
      this.newMethodDescriptor = newMD;
    }

    public String getKey() {
      return methodName + " " + orgMethodDescriptor;
    }
  }

  public static class FunctionInfo {
    public final Map<String, GenericFunctionInfo> genericFunctions = new HashMap<>();
    public final Map<String, AppliedFunctionInfo> appliedFunctions = new HashMap<>();
  }
}
