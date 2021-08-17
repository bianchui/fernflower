package com.github.bianchui.ff.generic;

import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;

public class GenericUtil {
  public static String toDesc(GenericType type) {
    return (new VarType(type.type, type.arrayDim, type.value)).toString();
  }
}
